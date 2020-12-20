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
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

/**
 * Bean representing a key/value context parameter.
 *
 * @author cvarela
 * @since 0.2
 */
public class ContextBean implements Serializable {

    private String activation;
    private String key;
    private String unset;
    private String value;

    public String getActivation() {
        return activation;
    }

    @XmlAttribute
    public void setActivation(String activation) {
        this.activation = BeanHelper.normalize(activation);
    }

    public String getKey() {
        return key;
    }

    @XmlAttribute(required = true)
    public void setKey(String key) {
        this.key = BeanHelper.normalize(key);
    }

    public String getUnset() {
        return unset;
    }

    @XmlAttribute
    public void setUnset(String unset) {
        this.unset = BeanHelper.normalize(unset);
    }

    public String getValue() {
        return value;
    }

    @XmlAttribute(required = true)
    public void setValue(String value) {
        this.value = BeanHelper.normalize(value);
    }
}
