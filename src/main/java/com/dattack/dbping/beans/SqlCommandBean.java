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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;

/**
 * @author cvarela
 * @since 0.1
 */
public abstract class SqlCommandBean implements Serializable {

    private static final long serialVersionUID = 6216650272101740792L;

    @XmlElement(name = "context")
    private List<ContextBean> contextBeanList;

    public List<ContextBean> getContextBeanList() {
        return contextBeanList == null ? Collections.emptyList() : contextBeanList;
    }

    public abstract <T extends Throwable> void accept(final SqlCommandVisitor<T> visitor) throws T;

    /**
     * @return the label
     */
    public abstract String getLabel();

    /**
     * @return the weight
     */
    public abstract float getWeight();
}
