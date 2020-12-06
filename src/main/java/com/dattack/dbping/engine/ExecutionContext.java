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

import com.dattack.dbping.beans.PingTaskBean;
import com.dattack.dbping.log.LogWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.SQLException;
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
    private long iteration = 0;

    /**
     * Creates a new instance with the values provided.
     *
     * @param pingTaskBean the task to be executed
     * @param dataSource   the data source that provides the connections
     * @param logWriter    the log in which to write the metrics obtained
     */
    public ExecutionContext(final PingTaskBean pingTaskBean, final DataSource dataSource, final LogWriter logWriter) {
        this.pingTaskBean = pingTaskBean;
        this.dataSource = dataSource;
        this.logWriter = logWriter;
        final String threadName = Thread.currentThread().getName();
        this.logEntryBuilder = new LogEntry.LogEntryBuilder(pingTaskBean.getMaxRowsToDump()) //
                .withTaskName(pingTaskBean.getName()) //
                .withThreadName(threadName);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
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
}
