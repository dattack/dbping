/*
 * Copyright (c) 2020, The Dattack team (http://www.dattack.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dattack.dbping.engine;

import com.dattack.dbping.beans.AbstractSqlParameterBean;
import com.dattack.dbping.beans.ClusterAbstractSqlParameterBean;
import com.dattack.dbping.beans.SimpleAbstractSqlParameterBean;
import com.dattack.dbping.beans.SqlParameterBeanVisitor;
import com.dattack.dbping.beans.SqlStatementBean;
import com.dattack.jtoolbox.commons.configuration.ConfigurationUtil;
import com.dattack.jtoolbox.exceptions.DattackNestableRuntimeException;
import com.dattack.jtoolbox.jdbc.JDBCUtils;
import org.apache.commons.lang.exception.NestableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * SQL statement executable in the database using a {@link PreparedStatement}. Before launching the execution it is
 * necessary to assign the corresponding values to the parameters of the sentence.
 *
 * @author cvarela
 * @since 0.2
 */
public class ExecutablePreparedStatement extends ExecutableStatement implements SqlParameterBeanVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutablePreparedStatement.class);

    private final List<AbstractPreparedStatementParameter<?>> parameterList = new ArrayList<>();

    /**
     * Default constructor.
     *
     * @param bean the configuration object
     * @throws IOException if an error occurs when reading parameter values
     */
    public ExecutablePreparedStatement(SqlStatementBean bean) throws IOException {
        super(bean);

        for (AbstractSqlParameterBean parameterBean : bean.getParameterList()) {
            try {
                parameterBean.accept(this);
            } catch (DattackNestableRuntimeException e) {
                throw (IOException) e.getCause(); // TODO: improve it
            }
        }

        parameterList.sort(Comparator.comparingInt(AbstractPreparedStatementParameter::getOrder));
    }

    @Override
    public void visit(SimpleAbstractSqlParameterBean bean) {
        try {
            parameterList.add(new SimplePreparedStatementParameter(bean));
        } catch (IOException e) {
            throw new DattackNestableRuntimeException(e);
        }
    }

    @Override
    public void visit(ClusterAbstractSqlParameterBean bean) {
        try {
            parameterList.add(new ClusterPreparedStatementParameter(bean));
        } catch (IOException e) {
            throw new DattackNestableRuntimeException(e);
        }
    }

    @Override
    public void execute(final ExecutionContext context) {

        // prepare log for this execution
        context.getLogEntryBuilder() //
                .init() //
                .withSqlLabel(ConfigurationUtil.interpolate(getBean().getLabel(), context.getConfiguration())) //
                .withIteration(context.getIteration());

        try (Connection connection = context.getConnection()) {

            populateClientInfo(connection);

            // sets the connection time
            context.getLogEntryBuilder().connect();

            try (PreparedStatement stmt = connection.prepareStatement(compileSql(context))) {
                doExecute(context, stmt);
            }
        } catch (final Exception e) {
            context.getLogWriter().write(context.getLogEntryBuilder().withException(e).build());
            LOGGER.warn("Job error (Name: {}, Statement: {}'): {}", context.getName(), getBean().getLabel(),
                    e.getMessage());
        }
    }

    private void setClientInfo(final Connection connection, final String key, final String value) {
        try {
            connection.setClientInfo(key, value);
        } catch (SQLException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    private void populateClientInfo(Connection connection) {
        setClientInfo(connection, "OCSID.CLIENTID", "DBPING_ID");
        setClientInfo(connection, "OCSID.MODULE", "DBPING");
        setClientInfo(connection, "OCSID.ACTION", "TEST");
    }

    public void execute(final ExecutionContext context, Connection connection) throws NestableException {

        populateClientInfo(connection);

        // prepare log for this execution
        context.getLogEntryBuilder() //
                .init() //
                .withSqlLabel(ConfigurationUtil.interpolate(getBean().getLabel(), context.getConfiguration())) //
                .withIteration(context.getIteration()) //
                .connect(); // connection already established so the connection-time must be zero

        try (PreparedStatement stmt = connection.prepareStatement(compileSql(context))) {
            doExecute(context, stmt);
        } catch (Exception e) {
            throw new NestableException(e);
        }
    }

    private void doExecute(final ExecutionContext context, PreparedStatement stmt) throws NestableException {

        ResultSet resultSet = null;
        try {
            populatePreparedStatement(context, stmt);
            setFetchSize(stmt);

            boolean executeResult = stmt.execute();

            if (executeResult) {
                resultSet = stmt.getResultSet();
                while (resultSet.next()) {
                    // sets the time for the first row
                    context.getLogEntryBuilder().addRow(resultSet);
                }
            }

            // sets the total run time; then, writes to log
            writeResults(context);
        } catch (Exception e) {
            throw new NestableException(e);
        } finally {
            JDBCUtils.closeQuietly(resultSet);
        }
    }

    private void populatePreparedStatement(final ExecutionContext context,
                                           final PreparedStatement statement)
            throws SQLException, ParseException {

        StringBuilder parametersComment = new StringBuilder();

        int paramIndex = 1;
        for (AbstractPreparedStatementParameter<?> parameter : parameterList) {

            if (paramIndex > 1) {
                parametersComment.append(",");
            }

            if (parameter instanceof SimplePreparedStatementParameter) {
                parametersComment.append(populatePreparedStatement(context, statement, paramIndex++,
                        (SimplePreparedStatementParameter) parameter));
            } else {
                ClusterPreparedStatementParameter clusterParameter = (ClusterPreparedStatementParameter) parameter;
                for (int iteration = 0; iteration < clusterParameter.getIterations(); iteration++) {
                    String[] values = clusterParameter.getValue();
                    for (SimplePreparedStatementParameter childParameter : clusterParameter.getParameterList()) {
                        parametersComment.append(populatePreparedStatement(context, statement, paramIndex++,
                                new SimplePreparedStatementParameter(childParameter.getBean(),
                                        values[childParameter.getRef()])));
                    }
                }
            }

        }

        context.getLogEntryBuilder().withComment(parametersComment.toString());
    }

    private String populatePreparedStatement(final ExecutionContext context,
                                             final PreparedStatement statement, int index,
                                             final SimplePreparedStatementParameter parameter)
            throws SQLException, ParseException {

        String value = parameter.getValue();
        switch (parameter.getType().toUpperCase()) {
            case "INTEGER":
                statement.setInt(index, Integer.parseInt(value));
                break;
            case "LONG":
                statement.setLong(index, Long.parseLong(value));
                break;
            case "FLOAT":
                statement.setFloat(index, Float.parseFloat(value));
                break;
            case "DOUBLE":
                statement.setDouble(index, Double.parseDouble(value));
                break;
            case "TIME":
                statement.setTime(index, new java.sql.Time(parseDate(value,
                        parameter.getFormat()).getTime()));
                break;
            case "DATE":
                statement.setDate(index, new java.sql.Date(parseDate(value, parameter.getFormat()).getTime()));
                break;
            case "TIMESTAMP":
                statement.setTimestamp(index,
                        new java.sql.Timestamp(parseDate(value, parameter.getFormat()).getTime()));
                break;
            case "STRING":
            default:
                statement.setString(index, value);
        }

        return " p" + index + " = " + value;
    }

    private Date parseDate(String value, String format) throws ParseException {
        SimpleDateFormat parser = new SimpleDateFormat(format);
        return parser.parse(value);
    }
}
