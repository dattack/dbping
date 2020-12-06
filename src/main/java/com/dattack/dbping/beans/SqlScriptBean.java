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

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * This is a script that contains several SQL statements that must be executed together.
 *
 * @author cvarela
 * @since 0.1
 */
public class SqlScriptBean implements SqlCommandBean {

    private static final long serialVersionUID = 5671689427608954154L;

    @XmlAttribute(name = "label", required = true)
    private String label;

    @XmlAttribute(name = "weight", required = false)
    private float weight = -1;

    @XmlElement(name = "query", required = true, type = SqlStatementBean.class)
    private List<SqlStatementBean> statementList;

    @Override
    public void accept(final SqlCommandVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Returns the label associated with the script.
     *
     * @return the label associated with the script
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * Returns the list of statements contained in this script.
     *
     * @return the statementList
     */
    public List<SqlStatementBean> getStatementList() {
        return statementList;
    }

    /**
     * Returns the weight assigned to this script.
     *
     * @return the weight assigned to this script
     */
    @Override
    public float getWeight() {
        return weight;
    }
}
