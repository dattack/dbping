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
import com.dattack.dbping.log.LogEntry;
import com.dattack.dbping.log.LogWriter;
import com.dattack.jtoolbox.commons.configuration.ConfigurationUtil;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * The execution context for a given task.
 *
 * @author cvarela
 * @since 0.1
 */
public final class ExecutionContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionContext.class);

    private static final String DBPING_PREFIX = "dbping.";

    @Deprecated
    public static final String DEPRECATED_PARENT_NAME_PROPERTY = "parent.name";
    public static final String PARENT_NAME_PROPERTY = DBPING_PREFIX + "parent.name";

    public static final String THREAD_NAME_PROPERTY = DBPING_PREFIX + "thread.name";
    public static final String THREAD_ID_PROPERTY = DBPING_PREFIX + "thread.id";

    @Deprecated
    public static final String DEPRECATED_NOW_PROPERTY = "now";
    public static final String NOW_PROPERTY = DBPING_PREFIX + "now";

    @Deprecated
    public static final String DEPRECATED_TASK_NAME_PROPERTY = "task.name";
    public static final String TASK_NAME_PROPERTY = DBPING_PREFIX + "task.name";

    @Deprecated
    public static final String DEPRECATED_DATASOURCE_PROPERTY = "datasource";
    public static final String DATASOURCE_PROPERTY = DBPING_PREFIX + "datasource";

    private static final String LAP_ID_PROPERTY = DBPING_PREFIX + "lap.id";

    private static final Map<String, String> PROPERTIES_MAPPING = new HashMap<>();

    static {
        PROPERTIES_MAPPING.put(PARENT_NAME_PROPERTY, DEPRECATED_PARENT_NAME_PROPERTY);
        PROPERTIES_MAPPING.put(NOW_PROPERTY, DEPRECATED_NOW_PROPERTY);
        PROPERTIES_MAPPING.put(TASK_NAME_PROPERTY, DEPRECATED_TASK_NAME_PROPERTY);
        PROPERTIES_MAPPING.put(DATASOURCE_PROPERTY, DEPRECATED_DATASOURCE_PROPERTY);

        PROPERTIES_MAPPING.put(DEPRECATED_PARENT_NAME_PROPERTY, PARENT_NAME_PROPERTY);
        PROPERTIES_MAPPING.put(DEPRECATED_NOW_PROPERTY, NOW_PROPERTY);
        PROPERTIES_MAPPING.put(DEPRECATED_TASK_NAME_PROPERTY, TASK_NAME_PROPERTY);
        PROPERTIES_MAPPING.put(DEPRECATED_DATASOURCE_PROPERTY, DATASOURCE_PROPERTY);
    }

    private final transient DataSource dataSource;
    private final transient PingTaskBean pingTaskBean;
    private final LogWriter logWriter;
    private final LogEntry.LogEntryBuilder logEntryBuilder;
    private final AbstractConfiguration configuration;
    private transient long lapId;

    public ExecutionContext(final ExecutionContext other) {
        this.pingTaskBean = other.pingTaskBean;
        this.dataSource = other.dataSource;
        this.logWriter = other.logWriter;
        this.logEntryBuilder = other.logEntryBuilder;
        this.configuration = new BaseConfiguration();
        ConfigurationUtils.copy(other.configuration, this.configuration);
        this.lapId = other.lapId;
    }

    /**
     * Creates a new instance with the values provided.
     *
     * @param pingTaskBean the task to be executed
     * @param dataSource   the data source that provides the connections
     * @param logWriter    the log in which to write the metrics obtained
     * @param configuration the execution configuration
     */
    public ExecutionContext(final PingTaskBean pingTaskBean, final DataSource dataSource, final LogWriter logWriter,
                            final AbstractConfiguration configuration) {
        this.pingTaskBean = pingTaskBean;
        this.dataSource = dataSource;
        this.logWriter = logWriter;
        this.configuration = configuration;
        final String threadName = Thread.currentThread().getName();
        this.logEntryBuilder = new LogEntry.LogEntryBuilder() //
                .withTaskName(pingTaskBean.getName()) //
                .withThreadName(threadName);
        this.lapId = 0;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public AbstractConfiguration getConfiguration() {
        configuration.setProperty(LAP_ID_PROPERTY, lapId);
        return configuration;
    }

    public String interpolate(String value) {
        return ConfigurationUtil.interpolate(value, getConfiguration());
    }

    public void setProperty(String key, Object value) {
        configuration.setProperty(key, value);
        String mapping = PROPERTIES_MAPPING.get(key);
        if (Objects.nonNull(mapping)) {
            configuration.setProperty(mapping, value);
        }
    }

    private void incrIteration() {
        lapId++;
    }

    public long getIteration() {
        return lapId;
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
        if (hasMoreIterations() && pingTaskBean.getTimeBetweenExecutions() > 0) {
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

    public boolean hasMoreIterations() {
        return pingTaskBean.getExecutions() <= 0 || lapId < pingTaskBean.getExecutions();
    }

    public void set(final List<ContextBean> list) {
        list.forEach(x -> getConfiguration().setProperty(x.getKey(), x.getValue()));
    }
}
