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
package com.dattack.dbping.beans;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Configuration of a parameter that can be used with a PreparedStatement.
 *
 * @author cvarela
 * @since 0.2
 */
public class ClusterSqlParameterBean extends AbstractSqlParameterBean {

    private static final long serialVersionUID = 6514464346366327811L;

    private String file;
    private List<SimpleSqlParameterBean> parameterList = new ArrayList<>();

    @Override
    public void accept(SqlParameterBeanVisitor visitor){
        visitor.visit(this);
    }

    public final String getFile() {
        return file;
    }

    @XmlAttribute(required = true)
    public void setFile(String file) {
        this.file = BeanHelper.normalizeToEmpty(file);
    }

    public List<SimpleSqlParameterBean> getParameterList() {
        return parameterList;
    }

    @XmlElement(name = "parameter", required = true)
    public void setParameterList(List<SimpleSqlParameterBean> parameterList) {
        this.parameterList = parameterList;
    }
}
