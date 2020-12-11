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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
public class SqlStatementBean implements SqlCommandBean {

    private static final long serialVersionUID = -5343761660462688691L;

    @XmlElement(name = "sql", required = true)
    private String sql;

    @XmlAttribute(name = "label", required = true)
    private String label;

    @XmlAttribute(name = "weight", required = false)
    private float weight = -1;

    @XmlAttribute(name = "fetchSize", required = false)
    private int fetchSize = -1;

    @XmlElements({ //
            @XmlElement(name = "parameter", type = SimpleAbstractSqlParameterBean.class), //
            @XmlElement(name = "cluster-parameter", type = ClusterAbstractSqlParameterBean.class) //
    })
    private List<AbstractSqlParameterBean> parameterList;

    @XmlAttribute(name = "forcePrepareStatement", required = false)
    private boolean forcePrepareStatement = true;

    @XmlAttribute(name = "ignoreMetrics", required = false)
    private boolean ignoreMetrics = false;

    public SqlStatementBean() {
        parameterList = new ArrayList<>();
    }

    @Override
    public void accept(final SqlCommandVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String getLabel() {
        return label;
    }

    /**
     * Returns the sql statement.
     *
     * @return the sql statement
     * @throws IOException when the content of the query is in a file and it is not possible to read from it
     */
    public String getSql() throws IOException {
        String fileProtocol = "file://";
        if (sql.trim().startsWith(fileProtocol)) {
            String path = sql.trim().substring(fileProtocol.length());
            sql = new String(Files.readAllBytes(Paths.get(path)));
        }
        return sql;

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

    /**
     * Returns the fetch size.
     *
     * @return the fetch size
     */
    public int getFetchSize() {
        return fetchSize;
    }

    /**
     * Returns the list of parameters used by this statement.
     *
     * @return the list of parameters used by this statement
     */
    public List<AbstractSqlParameterBean> getParameterList() {
        return parameterList;
    }

    /**
     * Returns a boolean indicating whether a PreparedStatement should be used to execute this SQL statement.
     *
     * @return a boolean indicating whether a PreparedStatement should be used to execute this SQL statement.
     */
    public boolean isForcePrepareStatement() {
        return forcePrepareStatement;
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
}
