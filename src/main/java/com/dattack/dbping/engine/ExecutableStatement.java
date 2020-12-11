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

import com.dattack.dbping.beans.ContextBean;
import com.dattack.dbping.beans.SqlStatementBean;
import com.dattack.jtoolbox.commons.configuration.ConfigurationUtil;
import com.dattack.jtoolbox.jdbc.JDBCUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.NestableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQL statement executable directly in the database using a {@link Statement}.
 *
 * @author cvarela
 * @since 0.2
 */
public class ExecutableStatement implements ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutableStatement.class);

    private final SqlStatementBean bean;

    public ExecutableStatement(SqlStatementBean bean) {
        this.bean = bean;
    }

    @Override
    public SqlStatementBean getBean() {
        return bean;
    }

    protected String compileSql(final ExecutionContext context) throws IOException {

        context.set(bean.getContextBeanList());
        String sql = ConfigurationUtil.interpolate(getBean().getSql(), context.getConfiguration());
        LOGGER.trace("Executing query {}", sql);
        return sql;
    }

    @Override
    public void execute(final ExecutionContext context) {

        context.getLogEntryBuilder() //
                .init() //
                .withSqlLabel(getBean().getLabel()) //
                .withIteration(context.getIteration()) //
                .withSqlLabel(getBean().getLabel());

        try (Connection connection = context.getConnection()) {

            context.getLogEntryBuilder().connect();

            try (Statement stmt = connection.createStatement()) {
                doExecute(context, stmt);
            }

        } catch (final Exception e) {
            context.getLogWriter().write(context.getLogEntryBuilder().withException(e).build());
            LOGGER.warn("Job error (Context: {}, Statement: {}'): {}", context.getName(), bean.getLabel(),
                    e.getMessage());
        }
    }

    /**
     * Executes this sentence and collects performance metrics.
     *
     * @param context    the execution context
     * @param connection the connection to the database
     * @throws NestableException if an error occurs when collecting the metrics
     */
    public void execute(final ExecutionContext context, Connection connection) throws NestableException {

        context.getLogEntryBuilder() //
                .init() //
                .withSqlLabel(getBean().getLabel()) //
                .withIteration(context.getIteration()) //
                .withSqlLabel(getBean().getLabel()) //
                .connect();

        try (Statement stmt = connection.createStatement()) {
            doExecute(context, stmt);
        } catch (Exception e) {
            throw new NestableException(e);
        }
    }

    private void doExecute(final ExecutionContext context, Statement stmt) throws SQLException, IOException {

        setFetchSize(stmt);

        ResultSet resultSet = null;
        try {
            boolean executeResult = stmt.execute(compileSql(context));

            if (executeResult) {
                resultSet = stmt.getResultSet();
                while (resultSet.next()) {
                    context.getLogEntryBuilder().addRow(resultSet);
                }
            }

            // sets the total time
            writeResults(context);
        } finally {
            JDBCUtils.closeQuietly(resultSet);
        }
    }

    protected final void setFetchSize(final Statement stmt) throws SQLException {
        if (bean.getFetchSize() > 0) {
            stmt.setFetchSize(bean.getFetchSize());
        }
    }

    protected final void writeResults(final ExecutionContext context) {
        if (!getBean().isIgnoreMetrics()) {
            context.getLogWriter().write(context.getLogEntryBuilder().build());
        }
    }
}
