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

import org.apache.commons.lang.StringUtils;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Bean representing a key/value context parameter. It is possible to indicate an activation condition and a default
 * value when this condition evaluates to false. The list of configurable properties are:
 *
 * <ul>
 * <li>key: the name of this property. This parameter is mandatory.</li>
 * <li>value: the value assigned to this property. This parameter is mandatory.</li>
 * <li>activation: the activation condition of this property. This parameter is optional.</li>
 * <li>unset: the value assigned to this property when the activation condition is false. This parameter is
 * optional (default: "")</li>
 * </ul>
 *
 * <p>Example:</p>
 *
 * <pre>{@code
 *     <context key="..." value="..." activation="..." unset="..." />
 * }</pre>
 *
 * @author cvarela
 * @since 0.2
 */
@SuppressWarnings("PMD.DataClass")
public class ContextBean implements Serializable {

    private static final long serialVersionUID = -1900734488693434419L;

    private String key;
    private String[] value;
    private int index = 0;

    public String getKey() {
        return key;
    }

    @XmlAttribute(required = true)
    public void setKey(final String key) {
        this.key = BeanHelper.normalizeToEmpty(key);
    }

    public synchronized String getValue() {

        if (value.length == 0) {
            return StringUtils.EMPTY;
        }
        if (index >= value.length) {
            index = 0;
        }
        return StringUtils.trimToEmpty(value[index++]);
    }

    @XmlAttribute(required = true)
    public void setValue(final String value) {
        this.value = BeanHelper.normalizeToEmpty(value).split(",");
    }
}
