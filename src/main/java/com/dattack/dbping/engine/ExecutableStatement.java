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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQL statement executable directly in the database using a {@link Statement}.
 *
 * @author cvarela
 * @since 0.2
 */
public final class ExecutableStatement extends AbstractExecutableStatement<Statement> implements ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutableStatement.class);

    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    public ExecutableStatement(final SqlStatementBean bean) {
        super(bean);
    }

    @Override
    public void execute(final ExecutionContext context) throws ExecutableException {

        context.getLogEntryBuilder() //
                .init() //
                .withIteration(context.getIteration()) //
                .withSqlLabel(ConfigurationUtil.interpolate(getBean().getLabel(), context.getConfiguration()));

        try (Connection connection = context.getConnection()) {

            context.getLogEntryBuilder().connect();

            try (Statement stmt = connection.createStatement()) {
                THREAD_LOCAL.set(compileSql(context));
                doExecute(context, stmt);
            }

        } catch (final Exception e) {
            context.getLogWriter().write(context.getLogEntryBuilder().withException(e).build());
            throwException(context, e);
            LOGGER.warn("Error executing statement ({}): {}", getBean().getLabel(), e.getMessage());
        }
    }

    /**
     * Executes this sentence and collects performance metrics.
     *
     * @param context    the execution context
     * @param connection the connection to the database
     * @throws ExecutableException if an error occurs when collecting the metrics
     */
    @Override
    public void execute(final ExecutionContext context, final Connection connection) throws ExecutableException {

        context.getLogEntryBuilder() //
                .init() //
                .withIteration(context.getIteration()) //
                .withSqlLabel(ConfigurationUtil.interpolate(getBean().getLabel(), context.getConfiguration())) //
                .withConnectionId(connection.hashCode()) //
                .connect();

        try (Statement stmt = connection.createStatement()) {
            THREAD_LOCAL.set(compileSql(context));
            doExecute(context, stmt);

        } catch (SQLException | IOException e) {
            context.getLogWriter().write(context.getLogEntryBuilder().withException(e).build());
            throwException(context, e);
        }
    }

    @Override
    protected void populateStatement(final Statement statement, final int index,
                                     final SimplePreparedStatementParameter parameter,
                                     final ParameterRecorder parameterRecorder, final ExecutionContext context)
            throws IOException {

        final String value = parameter.getValue(context);
        THREAD_LOCAL.set(THREAD_LOCAL.get().replaceFirst("\\?", value));
        parameterRecorder.save(index, value);
    }

    @Override
    protected void addBatch(final ExecutionContext context, final Statement stmt) throws SQLException {
        stmt.addBatch(THREAD_LOCAL.get());
    }

    @Override
    protected boolean executeStatement(final ExecutionContext context, final Statement stmt) throws SQLException {
        LOGGER.trace("Executing statement: {}", THREAD_LOCAL.get());
        return stmt.execute(THREAD_LOCAL.get());
    }
}
