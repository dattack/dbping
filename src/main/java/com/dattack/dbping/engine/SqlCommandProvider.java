/*
 * Copyright (c) 2014, The Dattack team (http://www.dattack.com)
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dattack.dbping.beans.SqlCommandBean;
import com.dattack.dbping.beans.SqlCommandVisitor;
import com.dattack.dbping.beans.SqlScriptBean;
import com.dattack.dbping.beans.SqlStatementBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the methods to be implemented by the provider of the SQL-query executed by a ping job.
 *
 * @author cvarela
 * @since 0.1
 */
public abstract class SqlCommandProvider implements SqlCommandVisitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlCommandProvider.class);

    private final List<ExecutableCommand> executableCommandList = new ArrayList<>();

    /**
     * Returns the next ExecutableCommand to execute.
     *
     * @return the next ExecutableCommand to execute
     */
    abstract ExecutableCommand nextSql();

    /**
     * Sets the list os sentences to be executed.
     *
     * @param sqlList the list of sentences to be executed.
     */
    final synchronized void setSentences(final List<SqlCommandBean> sqlList) {

        executableCommandList.clear();

        for (SqlCommandBean bean : sqlList) {
            bean.accept(this);
        }

        prepare(sqlList);
    }

    protected void prepare(final List<SqlCommandBean> sqlList) {
        // empty by default
    }

    protected final boolean isEmpty() {
        return executableCommandList.isEmpty();
    }

    protected final ExecutableCommand getCommand(final int index) {
        return executableCommandList.get(index);
    }

    protected final int getSize() {
        return executableCommandList.size();
    }

    @Override
    public void visit(final SqlScriptBean bean) {

        try {
            ExecutableScript executableScript = new ExecutableScript(bean);
            for (SqlStatementBean statement : bean.getStatementList()) {
                executableScript.add(createExecutableStatement(statement));
            }

            executableCommandList.add(executableScript);
        } catch (final Exception e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    @Override
    public void visit(final SqlStatementBean bean) {
        try {
            executableCommandList.add(createExecutableStatement(bean));
        } catch (final Exception e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    private ExecutableStatement createExecutableStatement(final SqlStatementBean bean) throws IOException {
        ExecutableStatement statement;
        if (bean.isForcePrepareStatement() || !bean.getParameterList().isEmpty()) {
            statement = new ExecutablePreparedStatement(bean);
        } else {
            statement = new ExecutableStatement(bean);
        }
        return statement;
    }
}
