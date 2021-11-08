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

import com.dattack.dbping.beans.SqlScriptBean;
import com.dattack.jtoolbox.commons.configuration.ConfigurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * An ordered list of SQL statements to be executed one after the other. All operations use the same connection but
 * do not use transactions; each operation is atomic but, in case of a failure, it does not rollback the failed
 * operation and continues with the next operation.
 *
 * @author cvarela
 * @since 0.2
 */
public class ExecutableScript implements ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutableScript.class);

    private final transient SqlScriptBean bean;
    private final transient List<AbstractExecutableStatement<?>> executableStatementList = new ArrayList<>();

    public ExecutableScript(final SqlScriptBean bean) {
        this.bean = bean;
    }

    public void add(final AbstractExecutableStatement<?> item) {
        this.executableStatementList.add(item);
    }

    public boolean isEmpty() {
        return executableStatementList.isEmpty();
    }

    @Override
    public SqlScriptBean getBean() {
        return bean;
    }

    /**
     * Executes all the sentences contained in this script using for all of them the same connection.
     *
     * @param context the execution context
     */
    @Override
    public void execute(final ExecutionContext context) {

        context.getLogEntryBuilder() //
                .init() //
                .withIteration(context.getIteration()) //
                .withSqlLabel(getBean().getLabel());

        try (Connection connection = context.getConnection()) {

            context.getLogEntryBuilder().connect();

            executableStatementList.forEach(s -> {
                try {
                    final ExecutionContext delegateContext = new ExecutionContext(context);
                    delegateContext.setProperty(ExecutionContext.PARENT_NAME_PROPERTY,
                            delegateContext.interpolate(bean.getLabel()));
                    if (!getBean().getContextBeanList().isEmpty()) {
                        delegateContext.set(getBean().getContextBeanList());
                    }
                    s.execute(delegateContext, connection);
                } catch (Exception e) {
                    log(context, e);
                }
            });

        } catch (final Exception e) {
            log(context, e);
        }
    }

    private void log(final ExecutionContext context, final Exception e) {
        context.getLogWriter().write(context.getLogEntryBuilder().withException(e).build());
        LOGGER.warn("Job error (Context: {}, Statement: {}'): {}", context.getName(), bean.getLabel(),
                e.getMessage());
        LOGGER.warn("Trace: ", e);
    }
}
