/*
 * Copyright (c) 2017, The Dattack team (http://www.dattack.com)
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
package com.dattack.dbping.beans;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

/**
 * @author cvarela
 * @since 0.1
 */
public class PingTaskBean implements Serializable {

    private static final long serialVersionUID = 3640559668991529501L;

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "threads", required = false)
    private int threads;

    @XmlAttribute(name = "executions", required = false)
    private int executions;

    @XmlAttribute(name = "timeBetweenExecutions", required = true)
    private int timeBetweenExecutions;

    @XmlAttribute(name = "datasource", required = true)
    private String datasource;

    @XmlAttribute(name = "maxRowsToDump", required = false)
    private int maxRowsToDump;

    @XmlElements({ @XmlElement(name = "query", type = SqlStatementBean.class),
        @XmlElement(name = "script", type = SqlScriptBean.class) })
    private List<SqlCommandBean> sqlStatementList;

    @XmlElement(name = "log-file", type = String.class)
    private String logFile;

    @XmlElement(name = "command-provider", type = String.class)
    private String commandProvider;

    /**
     * Returns the command provider.
     *
     * @return the commandProvider
     */
    public String getCommandProvider() {
        return commandProvider;
    }

    /**
     * Returns the datasource.
     *
     * @return the datasource
     */
    public String getDatasource() {
        return datasource;
    }

    /**
     * Returns the number of executions.
     *
     * @return the number of executions
     */
    public int getExecutions() {
        return executions;
    }

    /**
     * Returns the log file to use.
     *
     * @return the log file to use
     */
    public String getLogFile() {
        return logFile;
    }

    /**
     * Returns the maximum number of rows to be written in the log file for each iteration.
     *
     * @return the maxRowsToDump
     */
    public int getMaxRowsToDump() {
        return maxRowsToDump;
    }

    /**
     * Returns the task name.
     *
     * @return the task name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the list of sentences to be executed.
     *
     * @return the list of sentences to be executed
     */
    public List<SqlCommandBean> getSqlStatementList() {
        return sqlStatementList;
    }

    /**
     * The number of threads to be executed concurrently.
     *
     * @return the number of threads to be executed concurrently.
     */
    public int getThreads() {
        return threads;
    }

    /**
     * Returns the waiting time between two consecutive iterations.
     *
     * @return the waiting time between two consecutive iterations
     */
    public int getTimeBetweenExecutions() {
        return timeBetweenExecutions;
    }
}
