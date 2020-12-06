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

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Configuration of a parameter that can be used with a PreparedStatement.
 *
 * @author cvarela
 * @since 0.2
 */
public class SqlParameterBean implements Serializable {

    @XmlAttribute(name = "index", required = true)
    private int index;

    @XmlAttribute(name = "type", required = true)
    private String type;

    @XmlAttribute(name = "value", required = false)
    private String value;

    @XmlAttribute(name = "file", required = false)
    private String file;

    @XmlAttribute(name = "format", required = false)
    private String format;

    public String getValue() {
        return value;
    }

    public int getIndex() {
        return index;
    }

    public String getFormat() {
        return format;
    }

    public String getType() {
        return type;
    }

    public String getFile() {
        return file;
    }
}
