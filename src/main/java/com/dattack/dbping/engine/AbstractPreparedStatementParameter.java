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
package com.dattack.dbping.engine;

import com.dattack.dbping.beans.AbstractSqlParameterBean;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A parameter that can be substituted in a Statement. If the parameter supports multiple values,each access
 * to the parameter will get a different value using a Round-Robin algorithm.
 *
 * @author cvarela
 * @since 0.2
 */
public abstract class AbstractPreparedStatementParameter<T> {

    private final AbstractSqlParameterBean parameterBean;
    private final transient AtomicInteger valueIndex;
    private transient List<T> valueList;

    /**
     * Default constructor.
     *
     * @param parameterBean the bean containing the configuration of the parameter
     */
    public AbstractPreparedStatementParameter(final AbstractSqlParameterBean parameterBean) {
        this.parameterBean = parameterBean;
        this.valueIndex = new AtomicInteger(0);
    }

    public AbstractPreparedStatementParameter(final AbstractSqlParameterBean parameterBean, final List<T> valueList) {
        this.parameterBean = parameterBean;
        this.valueIndex = new AtomicInteger(0);
        this.valueList = valueList;
    }

    public final int getIterations() {
        return parameterBean.getIterations();
    }

    public final int getOrder() {
        return parameterBean.getOrder();
    }

    /**
     * Returns the next value to use within this parameter. If the parameter supports multiple values, each
     * invocation of this method will return a different value using a Round-Robin algorithm.
     *
     * @param context the execution context
     * @return returns the next value to use within this parameter.
     * @throws IOException if an I/O error occurs opening the configuration file
     */
    // TODO: refactoring needed (PMD.AvoidSynchronizedAtMethodLevel)
    public synchronized T getValue(final ExecutionContext context) throws IOException {
        if (Objects.isNull(valueList)) {
            valueList = loadValues(context);
        }

        if (valueList.isEmpty()) {
            return null;
        }

        final int index = valueIndex.getAndIncrement() % valueList.size();
        return valueList.get(index);
    }

    protected final AbstractSqlParameterBean getParameterBean() {
        return parameterBean;
    }

    protected abstract List<T> loadValues(final ExecutionContext context) throws IOException;
}
