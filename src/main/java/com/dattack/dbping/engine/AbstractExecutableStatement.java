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

import com.dattack.dbping.beans.AbstractSqlParameterBean;
import com.dattack.dbping.beans.BeanHelper;
import com.dattack.dbping.beans.ClusterSqlParameterBean;
import com.dattack.dbping.beans.SimpleSqlParameterBean;
import com.dattack.dbping.beans.SqlParameterBeanVisitor;
import com.dattack.dbping.beans.SqlStatementBean;
import com.dattack.dbping.engine.exceptions.ExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Abstract SQL statement executable.
 *
 * @author cvarela
 * @since 0.2
 */
@SuppressWarnings("PMD.TooManyMethods")
public abstract class AbstractExecutableStatement<T extends Statement> implements ExecutableCommand,
    SqlParameterBeanVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExecutableStatement.class);

    private final SqlStatementBean bean;
    private final transient List<AbstractPreparedStatementParameter<?>> parameterList = new ArrayList<>();

    public AbstractExecutableStatement(final SqlStatementBean bean) {
        this.bean = bean;
        for (final AbstractSqlParameterBean parameterBean : bean.getParameterList()) {
            parameterBean.accept(this);
        }

        parameterList.sort(Comparator.comparingInt(AbstractPreparedStatementParameter::getOrder));
    }

    public abstract void execute(final ExecutionContext context, final Connection connection)
        throws ExecutableException;

    @Override
    public final SqlStatementBean getBean() {
        return bean;
    }

    @Override
    public void visit(final SimpleSqlParameterBean bean) {
        parameterList.add(new SimplePreparedStatementParameter(bean));
    }

    @Override
    public void visit(final ClusterSqlParameterBean bean) {
        parameterList.add(new ClusterPreparedStatementParameter(bean));
    }

    protected abstract void addBatch(ExecutionContext context, T stmt) throws IOException, SQLException;

    protected final String compileSql(final ExecutionContext context) throws IOException {
        context.set(bean.getContextBeanList());
        final String sql = BeanHelper.getPlainSql(bean.getSql(), context.getConfiguration());
        LOGGER.trace("Executing query {}", sql);
        return sql;
    }

    // TODO: refactoring needed
    protected void doExecute(final ExecutionContext context, final T stmt) throws SQLException, IOException {

        context.getLogEntryBuilder().withConnectionId(stmt.getConnection().hashCode());

        int batchSize = 0;
        boolean batchMode = false;
        for (int iteration = 0; iteration < getBean().getRepeats(); iteration++) {
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

                final boolean executeResult = executeStatement(context, stmt);

                if (executeResult) {
                    try (ResultSet resultSet = stmt.getResultSet()) {
                        while (resultSet.next()) {
                            // sets the time for the first row
                            context.getLogEntryBuilder().addRow(resultSet, getBean().getMaxRowsToDump());
                        }
                    }
                }
            }
        }

        if (batchMode) {
            stmt.executeBatch();
        }

        // sets the total run time; then, writes to log
        writeResults(context);
    }

    protected abstract boolean executeStatement(ExecutionContext context, T stmt) throws IOException, SQLException;

    protected Date parseDate(final String value, final String format) throws ParseException {
        final SimpleDateFormat parser = new SimpleDateFormat(format, Locale.getDefault());
        return parser.parse(value);
    }

    protected abstract void populateStatement(final T statement, int index,
                                              final SimplePreparedStatementParameter parameter,
                                              final ParameterRecorder parameterRecorder,
                                              final ExecutionContext context)
        throws SQLException, ParseException, IOException;

    /* package */ final void throwException(final ExecutionContext context, final Exception e)
        throws ExecutableException {

        throw new ExecutableException(context.getName(), getBean().getLabel(),
            BeanHelper.normalizeToEmpty(getBean().getSql()),
            e);
    }

    // TODO: refactoring needed
    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "AccessorMethodGeneration"})
    private void doPopulateStatement(final ExecutionContext context, final T statement)
        throws SQLException, ParseException, IOException {

        final ParameterRecorder parameterRecorder = new ParameterRecorder();

        int paramIndex = 1;
        for (final AbstractPreparedStatementParameter<?> parameter : parameterList) {

            if (parameter instanceof SimplePreparedStatementParameter) {
                for (int iteration = 0; iteration < parameter.getIterations(); iteration++) {
                    populateStatement(statement, paramIndex++, (SimplePreparedStatementParameter) parameter,
                        parameterRecorder, context);
                }
            } else {
                final ClusterPreparedStatementParameter clusterParameter =
                    (ClusterPreparedStatementParameter) parameter;
                for (int iteration = 0; iteration < clusterParameter.getIterations(); iteration++) {
                    final String[] values = clusterParameter.getValue(context);
                    for (final SimplePreparedStatementParameter childParameter : clusterParameter.getParameterList()) {
                        populateStatement(statement, paramIndex++,
                            new SimplePreparedStatementParameter(childParameter.getBean(),
                                values[childParameter.getRef()]), parameterRecorder, context);
                    }
                }
            }
        }

        context.getLogEntryBuilder().withComment(parameterRecorder.getHash() + parameterRecorder.getLog());
    }

    private void prepare(final ExecutionContext context, final T stmt) throws SQLException,
        IOException {
        try {
            doPopulateStatement(context, stmt);
        } catch (ParseException e) {
            throw new SQLException(e);
        }
    }

    private void setFetchSize(final Statement stmt) throws SQLException {
        if (bean.getFetchSize() > 0) {
            stmt.setFetchSize(bean.getFetchSize());
        }
    }

    private void writeResults(final ExecutionContext context) {
        if (!getBean().isIgnoreMetrics()) {
            context.getLogWriter().write(context.getLogEntryBuilder().build());
        }
    }

    protected static class ParameterRecorder {

        // TODO: refactoring to remove this suppress warning (PMD.AvoidStringBufferField)
        @SuppressWarnings("PMD.AvoidStringBufferField")
        private final StringBuilder log;

        /* package */ ParameterRecorder() {
            this.log = new StringBuilder();
        }

        protected void save(final int index, final String value) {
            log.append(" p").append(index).append('=').append(value);
        }

        private String getHash() {
            return getLog().replaceAll("\\W", "");
        }

        private String getLog() {
            return log.toString();
        }
    }
}
