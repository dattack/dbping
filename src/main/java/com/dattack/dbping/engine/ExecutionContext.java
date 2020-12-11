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
import com.dattack.dbping.beans.PingTaskBean;
import com.dattack.dbping.log.LogWriter;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

/**
 * The execution context for a given task.
 *
 * @author cvarela
 * @since 0.1
 */
public final class ExecutionContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionContext.class);

    private final DataSource dataSource;
    private final PingTaskBean pingTaskBean;
    private final LogWriter logWriter;
    private final LogEntry.LogEntryBuilder logEntryBuilder;
    private final AbstractConfiguration configuration;
    private long iteration = 0;

    public ExecutionContext(ExecutionContext other) {
        this.pingTaskBean = other.pingTaskBean;
        this.dataSource = other.dataSource;
        this.logWriter = other.logWriter;
        this.logEntryBuilder = other.logEntryBuilder;
        this.configuration = new BaseConfiguration();
        ConfigurationUtils.copy(other.configuration, this.configuration);
        this.iteration = other.iteration;
    }

    /**
     * Creates a new instance with the values provided.
     *
     * @param pingTaskBean the task to be executed
     * @param dataSource   the data source that provides the connections
     * @param logWriter    the log in which to write the metrics obtained
     */
    public ExecutionContext(final PingTaskBean pingTaskBean, final DataSource dataSource, final LogWriter logWriter,
                            AbstractConfiguration configuration) {
        this.pingTaskBean = pingTaskBean;
        this.dataSource = dataSource;
        this.logWriter = logWriter;
        this.configuration = configuration;
        final String threadName = Thread.currentThread().getName();
        this.logEntryBuilder = new LogEntry.LogEntryBuilder(pingTaskBean.getMaxRowsToDump()) //
                .withTaskName(pingTaskBean.getName()) //
                .withThreadName(threadName);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public AbstractConfiguration getConfiguration() {
        return configuration;
    }

    private void incrIteration() {
        iteration++;
    }

    public long getIteration() {
        return iteration;
    }

    public LogWriter getLogWriter() {
        return logWriter;
    }

    public LogEntry.LogEntryBuilder getLogEntryBuilder() {
        return logEntryBuilder;
    }

    /**
     * Causes the current thread wait for the time indicated in {@link PingTaskBean#getTimeBetweenExecutions()}.
     */
    public void sleep() {
        if (isAlive() && pingTaskBean.getTimeBetweenExecutions() > 0) {
            synchronized (this) {
                try {
                    wait(pingTaskBean.getTimeBetweenExecutions());
                } catch (final InterruptedException e) {
                    LOGGER.warn(e.getMessage());
                }
            }
        }
        incrIteration();
    }

    public String getName() {
        return Thread.currentThread().getName() + "@" + pingTaskBean.getName();
    }

    public boolean isAlive() {
        return pingTaskBean.getExecutions() <= 0 || iteration < pingTaskBean.getExecutions();
    }

    public boolean test(String activation) {
        return StringUtils.isBlank(activation)
                || ("EVEN".equalsIgnoreCase(activation) && getIteration() % 2 == 0)
                || ("ODD".equalsIgnoreCase(activation) && getIteration() % 2 != 0);
    }

    public void set(final List<ContextBean> list) {
        list.forEach(x -> {
            if (test(x.getActivation())) {
                getConfiguration().setProperty(x.getKey(), x.getValue());
            } else if (StringUtils.isNotBlank(x.getUnset())) {
                getConfiguration().setProperty(x.getKey(), x.getUnset());
            }
        });
    }
}
