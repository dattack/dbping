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

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Configuration of a parameter that can be used with a PreparedStatement.
 *
 * @author cvarela
 * @since 0.2
 */
public class ClusterAbstractSqlParameterBean extends AbstractSqlParameterBean {

    @XmlAttribute(name = "iterations")
    private int iterations = 1;

    @XmlAttribute(name = "file", required = true)
    private String file;

    @XmlElement(name = "parameter", required = true)
    private List<SimpleAbstractSqlParameterBean> parameterList;

    public int getIterations() {
        return iterations;
    }

    public final String getFile() {
        return file;
    }

    public List<SimpleAbstractSqlParameterBean> getParameterList() {
        return parameterList;
    }

    @Override
    public <T extends Throwable> void accept(SqlParameterBeanVisitor<T> visitor) throws T {
        visitor.visit(this);
    }
}
