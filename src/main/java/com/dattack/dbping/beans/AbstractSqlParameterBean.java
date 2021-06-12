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
@SuppressWarnings("PMD.DataClass")
public abstract class AbstractSqlParameterBean implements Serializable {

    private static final long serialVersionUID = -6189064500896338334L;

    private int iterations = 1;
    private int order;

    public abstract void accept(SqlParameterBeanVisitor visitor);

    public int getIterations() {
        return iterations;
    }

    @XmlAttribute
    public void setIterations(final int iterations) {
        this.iterations = Math.max(iterations, 1);
    }

    public final int getOrder() {
        return order;
    }

    @XmlAttribute(required = true)
    public void setOrder(final int order) {
        this.order = order;
    }
}