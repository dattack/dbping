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

import com.dattack.dbping.beans.PingTaskBean;
import com.dattack.dbping.engine.exceptions.ExecutableException;
import com.dattack.dbping.log.LogWriter;
import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;

/**
 * Executes a ping-job instance.
 *
 * @author cvarela
 * @since 0.1
 */
class PingJob implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PingJob.class);

    private final transient PingTaskBean pingTaskBean;
    private final transient DataSource dataSource;
    private final transient SqlCommandProvider sentenceProvider;
    private final transient LogWriter logWriter;
    private final transient AbstractConfiguration configuration;

    public PingJob(final PingTaskBean pingTaskBean, final DataSource dataSource,
                   final SqlCommandProvider sentenceProvider, final LogWriter logWriter,
                   final AbstractConfiguration configuration) {

        this.pingTaskBean = pingTaskBean;
        this.dataSource = dataSource;
        this.sentenceProvider = sentenceProvider;
        this.logWriter = logWriter;
        this.configuration = configuration;
    }

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void run() {

        final ExecutionContext context = new ExecutionContext(pingTaskBean, dataSource, logWriter, configuration);
        context.set(pingTaskBean.getContextBeanList());
        context.getConfiguration().setProperty(ExecutionContext.PARENT_NAME_PROPERTY, pingTaskBean.getName());
        LOGGER.info("Starting job: {}", context.getName());

        while (context.hasMoreIterations()) {
            try {
                final ExecutableCommand executableCommand = sentenceProvider.nextSql();
                executableCommand.execute(new ExecutionContext(context));
                context.sleep();
            } catch (ExecutableException e) {
                LOGGER.warn(String.format("Job %s:", context.getName()), e);
            }
        }

        LOGGER.info("Job finished: {}", context.getName());
    }
}
