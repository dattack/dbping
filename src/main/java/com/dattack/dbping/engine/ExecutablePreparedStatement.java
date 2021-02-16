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
import com.dattack.dbping.engine.exceptions.ExecutableException;
import com.dattack.jtoolbox.commons.configuration.ConfigurationUtil;
import com.dattack.jtoolbox.exceptions.DattackNestableRuntimeException;
import com.dattack.jtoolbox.jdbc.JDBCUtils;
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
public class ExecutablePreparedStatement extends ExecutableStatement implements SqlParameterBeanVisitor<IOException> {

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
    public void visit(SimpleAbstractSqlParameterBean bean) throws IOException {
        parameterList.add(new SimplePreparedStatementParameter(bean));
    }

    @Override
    public void visit(ClusterAbstractSqlParameterBean bean) throws IOException {
        parameterList.add(new ClusterPreparedStatementParameter(bean));
    }

    @Override
    public void execute(final ExecutionContext context) throws ExecutableException {

        // prepare log for this execution
        context.getLogEntryBuilder() //
                .init() //
                .withSqlLabel(ConfigurationUtil.interpolate(getBean().getLabel(), context.getConfiguration())) //
                .withIteration(context.getIteration());

        try (Connection connection = context.getConnection()) {

            populateClientInfo(connection);

            // sets the connection time
            context.getLogEntryBuilder().connect();

            String sql = compileSql(context);
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                doExecute(context, stmt);
            }
        } catch (final Exception e) {
            context.getLogWriter().write(context.getLogEntryBuilder().withException(e).build());
            throwException(context, e);
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

    public void execute(final ExecutionContext context, Connection connection) throws ExecutableException {

        populateClientInfo(connection);

        // prepare log for this execution
        context.getLogEntryBuilder() //
                .init() //
                .withSqlLabel(ConfigurationUtil.interpolate(getBean().getLabel(), context.getConfiguration())) //
                .withIteration(context.getIteration()) //
                .withConnectionId(connection.hashCode()) //
                .connect(); // connection already established so the connection-time must be zero

        try {
            String sql = compileSql(context);
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                doExecute(context, stmt);
            } catch (SQLException | ParseException | IOException e) {
                throw new ExecutableException(context.getName(), getBean().getLabel(), sql, e);
            }
        } catch (IOException e) {
            throwException(context, e);
        }
    }

    private void doExecute(final ExecutionContext context, PreparedStatement stmt) throws SQLException,
            ParseException, IOException {

        context.getLogEntryBuilder().withConnectionId(stmt.getConnection().hashCode());

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
        } finally {
            JDBCUtils.closeQuietly(resultSet);
        }
    }

    private void populatePreparedStatement(final ExecutionContext context,
                                           final PreparedStatement statement)
            throws SQLException, ParseException, IOException {

        ParameterRecorder parameterRecorder = new ParameterRecorder();

        int paramIndex = 1;
        for (AbstractPreparedStatementParameter<?> parameter : parameterList) {

            if (parameter instanceof SimplePreparedStatementParameter) {
                populatePreparedStatement(statement, paramIndex++, (SimplePreparedStatementParameter) parameter,
                        parameterRecorder, context);
            } else {
                ClusterPreparedStatementParameter clusterParameter = (ClusterPreparedStatementParameter) parameter;
                for (int iteration = 0; iteration < clusterParameter.getIterations(); iteration++) {
                    String[] values = clusterParameter.getValue(context);
                    for (SimplePreparedStatementParameter childParameter : clusterParameter.getParameterList()) {
                        populatePreparedStatement(statement, paramIndex++,
                                new SimplePreparedStatementParameter(childParameter.getBean(),
                                        values[childParameter.getRef()]), parameterRecorder, context);
                    }
                }
            }
        }

        context.getLogEntryBuilder().withComment(parameterRecorder.getHash() + parameterRecorder.getLog());
    }

    private void populatePreparedStatement(final PreparedStatement statement, int index,
                                           final SimplePreparedStatementParameter parameter,
                                           final ParameterRecorder parameterRecorder,
                                           final ExecutionContext context)
            throws SQLException, ParseException, IOException {

        String value = parameter.getValue(context);
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

        parameterRecorder.save(index, value);
    }

    private Date parseDate(String value, String format) throws ParseException {
        SimpleDateFormat parser = new SimpleDateFormat(format);
        return parser.parse(value);
    }

    private static class ParameterRecorder {

        private final StringBuilder log;

        private ParameterRecorder() {
            this.log = new StringBuilder();
        }

        private void save(int index, String value) {
            log.append(" p").append(index).append("=").append(value);
        }

        private String getHash() {
            //return bytesToHex(getLog().replaceAll("\\W", "").getBytes(StandardCharsets.UTF_8));
            return getLog().replaceAll("\\W", "");
        }

        private static String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }

        private String getLog() {
            return log.toString();
        }
    }
}
