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

import com.dattack.dbping.beans.SqlStatementBean;
import com.dattack.dbping.engine.exceptions.ExecutableException;
import com.dattack.jtoolbox.commons.configuration.ConfigurationUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;

/**
 * SQL statement executable in the database using a {@link PreparedStatement}. Before launching the execution it is
 * necessary to assign the corresponding values to the parameters of the sentence.
 *
 * @author cvarela
 * @since 0.2
 */
public class ExecutablePreparedStatement extends AbstractExecutableStatement<PreparedStatement> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutablePreparedStatement.class);

    /**
     * Default constructor.
     *
     * @param bean the configuration object
     * @throws IOException if an error occurs when reading parameter values
     */
    public ExecutablePreparedStatement(SqlStatementBean bean) throws IOException {
        super(bean);
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
            LOGGER.warn(e.getMessage() + "[Key: " + key + ", Value: " + value + "]");
        }
    }

    private void populateClientInfo(Connection connection) {

        try {
            if (StringUtils.containsIgnoreCase(connection.getMetaData().getDriverName(), "oracle")) {
                setClientInfo(connection, "OCSID.CLIENTID", "DBPING_ID");
                setClientInfo(connection, "OCSID.MODULE", "DBPING");
                setClientInfo(connection, "OCSID.ACTION", "TEST");
            }
        } catch (SQLException e) {
            LOGGER.warn(e.getMessage());
        }
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
            } catch (SQLException | IOException e) {
                throw new ExecutableException(context.getName(), getBean().getLabel(), sql, e);
            }
        } catch (IOException e) {
            throwException(context, e);
        }
    }

    protected void addBatch(ExecutionContext context, PreparedStatement stmt) throws SQLException {
        stmt.addBatch();
    }

    protected boolean executeStatement(ExecutionContext context, PreparedStatement stmt) throws SQLException {
        return stmt.execute();
    }

    protected void populateStatement(final PreparedStatement statement, int index,
                                           final SimplePreparedStatementParameter parameter,
                                           final ParameterRecorder parameterRecorder,
                                           final ExecutionContext context)
            throws SQLException, ParseException, IOException {

        LOGGER.trace("Setting parameter values (index: {}, type: {})", index, parameter.getType());
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
                statement.setTime(index, new java.sql.Time(parseDate(value, parameter.getFormat()).getTime()));
                break;
            case "DATE":
                statement.setDate(index, new java.sql.Date(parseDate(value, parameter.getFormat()).getTime()));
                break;
            case "TIMESTAMP":
                statement.setTimestamp(index, //
                        new java.sql.Timestamp(parseDate(value, parameter.getFormat()).getTime()));
                break;
            case "STRING":
            default:
                statement.setString(index, value);
        }

        parameterRecorder.save(index, value);
    }
}
