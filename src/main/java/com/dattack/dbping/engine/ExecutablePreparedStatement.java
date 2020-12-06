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

import com.dattack.dbping.beans.SqlParameterBean;
import com.dattack.dbping.beans.SqlStatementBean;
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
public class ExecutablePreparedStatement extends ExecutableStatement {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutablePreparedStatement.class);

    private final List<PreparedStatementParameter> parameterList = new ArrayList<>();

    /**
     * Default constructor.
     *
     * @param bean the configuration object
     * @throws IOException if an error occurs when reading parameter values
     */
    public ExecutablePreparedStatement(SqlStatementBean bean) throws IOException {
        super(bean);

        for (SqlParameterBean parameterBean : bean.getParameterList()) {
            parameterList.add(new PreparedStatementParameter(parameterBean));
        }

        parameterList.sort(Comparator.comparingInt(PreparedStatementParameter::getIndex));
    }

    @Override
    public void execute(final ExecutionContext context) {

        // prepare log for this execution
        context.getLogEntryBuilder() //
                .init() //
                .withSqlLabel(getBean().getLabel()) //
                .withIteration(context.getIteration()) //
                .withSqlLabel(getBean().getLabel());

        try (Connection connection = context.getConnection()) {

            // sets the connection time
            context.getLogEntryBuilder().connect();

            try (PreparedStatement stmt = connection.prepareStatement(getBean().getSql())) {
                doExecute(context, stmt);
            }
        } catch (final Exception e) {
            context.getLogWriter().write(context.getLogEntryBuilder().withException(e).build());
            LOGGER.warn("Job error (Name: {}, Statement: {}'): {}", context.getName(), getBean().getLabel(),
                    e.getMessage());
        }
    }

    public void execute(final ExecutionContext context, Connection connection) throws NestableException {

        // prepare log for this execution
        context.getLogEntryBuilder() //
                .init() //
                .withSqlLabel(getBean().getLabel()) //
                .withIteration(context.getIteration()) //
                .withSqlLabel(getBean().getLabel())
                .connect();

        try (PreparedStatement stmt = connection.prepareStatement(getBean().getSql())) {
            doExecute(context, stmt);
        } catch (SQLException e) {
            throw new NestableException(e);
        }
    }

    private void doExecute(final ExecutionContext context, PreparedStatement stmt) throws NestableException {

        ResultSet resultSet = null;
        try {
            populatePreparedStatement(context, stmt);

            boolean executeResult = stmt.execute();

            if (executeResult) {
                resultSet = stmt.getResultSet();
                while (resultSet.next()) {
                    // sets the time for the first row
                    context.getLogEntryBuilder().addRow(resultSet);
                }
            }

            // sets the total run time; then, writes to log
            context.getLogWriter().write(context.getLogEntryBuilder().build());
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

        for (PreparedStatementParameter parameter : parameterList) {
            String value = parameter.getValue();
            if (parameter.getIndex() > 1) {
                parametersComment.append(",");
            }
            parametersComment.append(" p").append(parameter.getIndex()).append(" = ").append(value);
            switch (parameter.getType().toUpperCase()) {
                case "INTEGER":
                    statement.setInt(parameter.getIndex(), Integer.parseInt(value));
                    break;
                case "LONG":
                    statement.setLong(parameter.getIndex(), Long.parseLong(value));
                    break;
                case "FLOAT":
                    statement.setFloat(parameter.getIndex(), Float.parseFloat(value));
                    break;
                case "DOUBLE":
                    statement.setDouble(parameter.getIndex(), Double.parseDouble(value));
                    break;
                case "TIME":
                    statement.setTime(parameter.getIndex(), new java.sql.Time(parseDate(value,
                            parameter.getFormat()).getTime()));
                    break;
                case "DATE":
                    statement.setDate(parameter.getIndex(), new java.sql.Date(parseDate(value,
                            parameter.getFormat()).getTime()));
                    break;
                case "TIMESTAMP":
                    statement.setTimestamp(parameter.getIndex(),
                            new java.sql.Timestamp(parseDate(value, parameter.getFormat()).getTime()));
                    break;
                case "STRING":
                default:
                    statement.setString(parameter.getIndex(), value);
            }
        }

        context.getLogEntryBuilder().withComment(parametersComment.toString());
    }

    private Date parseDate(String value, String format) throws ParseException {
        SimpleDateFormat parser = new SimpleDateFormat(format);
        return parser.parse(value);
    }
}
