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
import javax.xml.bind.annotation.XmlElement;

/**
 * This is a script that contains several SQL statements that must be executed together.
 *
 * @author cvarela
 * @since 0.1
 */
@SuppressWarnings("PMD.DataClass")
public class SqlScriptBean extends SqlCommandBean {

    private static final long serialVersionUID = 5671689427608954154L;

    private List<SqlStatementBean> statementList = new ArrayList<>();

    @Override
    public <T extends Throwable> void accept(final SqlCommandVisitor<T> visitor) throws T {
        visitor.visit(this);
    }

    /**
     * Returns the list of statements contained in this script.
     *
     * @return the statementList
     */
    public List<SqlStatementBean> getStatementList() {
        return statementList;
    }

    @XmlElement(name = "query", required = true, type = SqlStatementBean.class)
    public void setStatementList(final List<SqlStatementBean> statementList) {
        this.statementList = statementList;
    }
}
