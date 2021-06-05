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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

/**
 * Bean representing a SQL statement.
 *
 * @author cvarela
 * @since 0.1
 */
public class SqlStatementBean extends SqlCommandBean {

    private static final long serialVersionUID = -5343761660462688691L;

    private int fetchSize = -1;
    private boolean ignoreMetrics = false;
    private String label;
    private List<AbstractSqlParameterBean> parameterList = new ArrayList<>();
    private boolean skip = false;
    private String sql;
    private boolean usePreparedStmt = true;
    private float weight = -1;
    private int repeats = 1;
    private int batchSize = 1;

    @Override
    public <T extends Throwable> void accept(final SqlCommandVisitor<T> visitor) throws T {
        visitor.visit(this);
    }

    @Override
    public String getLabel() {
        return BeanHelper.normalizeToEmpty(label);
    }

    @XmlAttribute(required = true)
    public void setLabel(final String label) {
        this.label = label;
    }

    /**
     * Returns the weight assigned to this statement.
     *
     * @return the weight assigned to this statement
     */
    @Override
    public float getWeight() {
        return weight;
    }

    @XmlAttribute
    public void setWeight(final float weight) {
        this.weight = weight;
    }

    @XmlAttribute
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getRepeats() {
        return repeats;
    }

    @XmlAttribute
    public void setRepeats(int repeats) {
        this.repeats = repeats;
    }

    /**
     * Returns the fetch size.
     *
     * @return the fetch size
     */
    public int getFetchSize() {
        return fetchSize;
    }

    @XmlAttribute
    public void setFetchSize(final int fetchSize) {
        this.fetchSize = fetchSize;
    }

    /**
     * Returns the list of parameters used by this statement.
     *
     * @return the list of parameters used by this statement
     */
    public List<AbstractSqlParameterBean> getParameterList() {
        return parameterList;
    }

    @XmlElements({ //
            @XmlElement(name = "parameter", type = SimpleSqlParameterBean.class), //
            @XmlElement(name = "cluster-parameter", type = ClusterSqlParameterBean.class) //
    })
    public void setParameterList(final List<AbstractSqlParameterBean> parameterList) {
        this.parameterList = parameterList;
    }

    /**
     * Returns the sql statement.
     *
     * @return the sql statement
     */
    public String getSql() {
        return sql;
    }

    @XmlElement(required = true)
    public void setSql(final String sql) {
        this.sql = BeanHelper.normalizeToEmpty(sql);
    }

    /**
     * returns True when the metrics obtained by executing this sentence should not be written in the result file;
     * otherwise, returns False.
     *
     * @return True when the metrics obtained by executing this sentence should not be written in the result file;
     * otherwise, returns False
     */
    public boolean isIgnoreMetrics() {
        return ignoreMetrics;
    }

    @XmlAttribute
    public void setIgnoreMetrics(final boolean ignoreMetrics) {
        this.ignoreMetrics = ignoreMetrics;
    }

    /**
     * returns True when this sentence should be skipped.
     *
     * @return True when this sentence should be skipped
     */
    public boolean isSkip() {
        return skip;
    }

    @XmlAttribute
    public void setSkip(final boolean skip) {
        this.skip = skip;
    }

    /**
     * Returns a boolean indicating whether a PreparedStatement should be used to execute this SQL statement.
     *
     * @return a boolean indicating whether a PreparedStatement should be used to execute this SQL statement.
     */
    public boolean isUsePreparedStatement() {
        return usePreparedStmt;
    }

    @XmlAttribute
    public void setUsePreparedStatement(final boolean usePreparedStmt) {
        this.usePreparedStmt = usePreparedStmt;
    }
}
