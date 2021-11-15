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

import com.dattack.dbping.engine.ExecutionContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Abstract class that executable commands inherit from.
 *
 * @author cvarela
 * @since 0.1
 */
public abstract class SqlCommandBean implements Serializable { //NOPMD

    private static final long serialVersionUID = 6216650272101740792L;

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private List<ContextBean> contextBeanList;
    private String label;
    private int maxRowsToDump;
    private float weight;

    public SqlCommandBean() {
        this.contextBeanList = new ArrayList<>();
        this.maxRowsToDump = 0;
        this.weight = -1;
        setLabel(String.format("${%s}.%d" , ExecutionContext.PARENT_NAME_PROPERTY, COUNTER.getAndIncrement()));
    }

    public abstract <T extends Throwable> void accept(final SqlCommandVisitor<T> visitor) throws T;

    public final List<ContextBean> getContextBeanList() {
        return contextBeanList;
    }

    @XmlElement(name = "context")
    public final void setContextBeanList(final List<ContextBean> contextBeanList) {
        if (Objects.nonNull(contextBeanList)) {
            this.contextBeanList = contextBeanList;
        }
    }

    public final String getLabel() {
        return BeanHelper.normalizeToEmpty(label);
    }

    @XmlAttribute(required = true)
    public final void setLabel(final String label) {
        BeanHelper.checkDeprecatedVariables(label, "label");
        this.label = label;
    }

    /**
     * Returns the maximum number of rows to be written in the log file for each iteration.
     *
     * @return the maxRowsToDump
     */
    public final int getMaxRowsToDump() {
        return maxRowsToDump;
    }

    @XmlAttribute
    public final void setMaxRowsToDump(final int maxRowsToDump) {
        this.maxRowsToDump = Math.max(0, maxRowsToDump);
    }

    public float getWeight() {
        return weight;
    }

    @XmlAttribute
    public void setWeight(final float weight) {
        this.weight = weight;
    }
}
