/*
 * Copyright (c) 2021, The Dattack team (http://www.dattack.com)
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

import com.dattack.dbping.beans.BeanHelper;
import com.dattack.dbping.beans.SqlStatementBean;
import com.dattack.dbping.engine.exceptions.ExecutableException;
import com.dattack.jtoolbox.jdbc.JDBCUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Abstract SQL statement executable.
 *
 * @author cvarela
 * @since 0.2
 */
public abstract class AbstractExecutableStatement<T extends Statement> implements ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExecutableStatement.class);

    private final SqlStatementBean bean;

    public AbstractExecutableStatement(SqlStatementBean bean) {
        this.bean = bean;
    }

    @Override
    public final SqlStatementBean getBean() {
        return bean;
    }

    protected final String compileSql(final ExecutionContext context) throws IOException {
        context.set(bean.getContextBeanList());
        String sql = BeanHelper.getPlainSql(bean.getSql(), context.getConfiguration());
        LOGGER.trace("Executing query {}", sql);
        return sql;
    }

    protected final void throwException(final ExecutionContext context, final Exception e) throws ExecutableException {
        throw new ExecutableException(context.getName(), getBean().getLabel(),
                BeanHelper.normalizeToEmpty(getBean().getSql()),
                e);
    }

    public abstract void execute(final ExecutionContext context, Connection connection) throws ExecutableException;

    protected void doExecute(final ExecutionContext context, T stmt) throws SQLException, IOException {

        context.getLogEntryBuilder().withConnectionId(stmt.getConnection().hashCode());

        int iteration = 1;
        int batchSize = 0;
        boolean batchMode = false;
        do {
            ResultSet resultSet = null;
            try {
                setFetchSize(stmt);
                prepare(context, stmt);

                if (getBean().getBatchSize() > 1 || getBean().getBatchSize() < 0) {
                    addBatch(context, stmt);
                    batchSize++;
                    batchMode = true;

                    if (getBean().getBatchSize() > 1 && (batchSize % getBean().getBatchSize() == 0)) {
                        stmt.executeBatch();
                        stmt.clearBatch();
                        batchMode = false;
                    }

                } else {

                    boolean executeResult = executeStatement(context, stmt);

                    if (executeResult) {
                        resultSet = stmt.getResultSet();
                        while (resultSet.next()) {
                            // sets the time for the first row
                            context.getLogEntryBuilder().addRow(resultSet, getBean().getMaxRowsToDump());
                        }
                    }
                }

            } finally {
                JDBCUtils.closeQuietly(resultSet);
            }
        } while (iteration++ < getBean().getRepeats());

        if (batchMode) {
            stmt.executeBatch();
        }

        // sets the total run time; then, writes to log
        writeResults(context);
    }

    protected abstract void prepare(ExecutionContext context, T stmt) throws IOException, SQLException;

    protected abstract void addBatch(ExecutionContext context, T stmt) throws IOException, SQLException;

    protected abstract boolean executeStatement(ExecutionContext context, T stmt) throws IOException, SQLException;

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
