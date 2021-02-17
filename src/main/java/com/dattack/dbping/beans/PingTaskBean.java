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
import java.util.ArrayList;
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

    private String commandProvider;
    private List<ContextBean> contextBeanList = new ArrayList<>();
    private String datasource;
    private int executions = -1;
    private String logFile;
    private String name;
    private List<SqlCommandBean> sqlStatementList = new ArrayList<>();
    private int threads = 1;
    private int timeBetweenExecutions;

    /**
     * Returns the command provider.
     *
     * @return the commandProvider
     */
    public String getCommandProvider() {
        return commandProvider;
    }

    @XmlElement(name = "command-provider")
    public void setCommandProvider(String commandProvider) {
        this.commandProvider = BeanHelper.normalizeToEmpty(commandProvider);
    }

    public List<ContextBean> getContextBeanList() {
        return contextBeanList;
    }

    @XmlElement(name = "context")
    public void setContextBeanList(List<ContextBean> contextBeanList) {
        this.contextBeanList = contextBeanList;
    }

    /**
     * Returns the datasource.
     *
     * @return the datasource
     */
    public String getDatasource() {
        return datasource;
    }

    @XmlAttribute(required = true)
    public void setDatasource(String datasource) {
        this.datasource = BeanHelper.normalizeToEmpty(datasource);
    }

    /**
     * Returns the number of executions.
     *
     * @return the number of executions
     */
    public int getExecutions() {
        return executions;
    }

    @XmlAttribute
    public void setExecutions(int executions) {
        this.executions = executions;
    }

    /**
     * Returns the log file to use.
     *
     * @return the log file to use
     */
    public String getLogFile() {
        return logFile;
    }

    @XmlElement(name = "log-file")
    public void setLogFile(String logFile) {
        this.logFile = BeanHelper.normalizeToEmpty(logFile);
    }

    /**
     * Returns the task name.
     *
     * @return the task name
     */
    public String getName() {
        return name;
    }

    @XmlAttribute(required = true)
    public void setName(String name) {
        this.name = BeanHelper.normalizeToEmpty(name);
    }

    /**
     * Returns the list of sentences to be executed.
     *
     * @return the list of sentences to be executed
     */
    public List<SqlCommandBean> getSqlStatementList() {
        return sqlStatementList;
    }

    @XmlElements({ @XmlElement(name = "query", type = SqlStatementBean.class),
            @XmlElement(name = "script", type = SqlScriptBean.class) })
    public void setSqlStatementList(List<SqlCommandBean> sqlStatementList) {
        this.sqlStatementList = sqlStatementList;
    }

    /**
     * The number of threads to be executed concurrently.
     *
     * @return the number of threads to be executed concurrently.
     */
    public int getThreads() {
        return threads;
    }

    @XmlAttribute
    public void setThreads(int threads) {
        this.threads = Math.max(threads, 1);
    }

    /**
     * Returns the waiting time between two consecutive iterations.
     *
     * @return the waiting time between two consecutive iterations
     */
    public int getTimeBetweenExecutions() {
        return timeBetweenExecutions;
    }

    @XmlAttribute(required = true)
    public void setTimeBetweenExecutions(int timeBetweenExecutions) {
        this.timeBetweenExecutions = timeBetweenExecutions;
    }
}
