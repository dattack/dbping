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

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Configuration of a parameter that can be used with a PreparedStatement.
 *
 * @author cvarela
 * @since 0.2
 */
public class SimpleSqlParameterBean extends AbstractSqlParameterBean {

    private static final long serialVersionUID = 144903240517619788L;

    private String file;
    private String format;
    private int ref;
    private String type;
    private String value;

    @Override
    public <T extends Throwable> void accept(SqlParameterBeanVisitor<T> visitor) throws T {
        visitor.visit(this);
    }

    public final String getFile() {
        return file;
    }

    @XmlAttribute
    public void setFile(String file) {
        this.file = BeanHelper.normalizeToEmpty(file);
    }

    public String getFormat() {
        return format;
    }

    @XmlAttribute
    public void setFormat(String format) {
        this.format = BeanHelper.normalizeToEmpty(format);
    }

    public int getRef() {
        return ref;
    }

    @XmlAttribute
    public void setRef(int ref) {
        this.ref = ref;
    }

    public String getType() {
        return type;
    }

    @XmlAttribute(required = true)
    public void setType(String type) {
        this.type = BeanHelper.normalizeToEmpty(type);
    }

    public String getValue() {
        return value;
    }

    @XmlAttribute
    public void setValue(String value) {
        this.value = BeanHelper.normalizeToEmpty(value);
    }
}
