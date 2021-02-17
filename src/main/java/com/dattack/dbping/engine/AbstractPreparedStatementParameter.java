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

/**
 * A parameter that can be substituted in a PreparedStatement. If the parameter supports multiple values,each access
 * to the parameter will get a different value using a Round-Robin algorithm.
 *
 * @author cvarela
 * @since 0.2
 */
public abstract class AbstractPreparedStatementParameter<T> {

    private final AbstractSqlParameterBean parameterBean;
    private List<T> valueList;
    private int valueIndex;

    /**
     * Default constructor.
     *
     * @param parameterBean the bean containing the configuration of the parameter
     */
    public AbstractPreparedStatementParameter(final AbstractSqlParameterBean parameterBean) {
        this.parameterBean = parameterBean;
        this.valueList = null;
        this.valueIndex = 0;
    }

    public AbstractPreparedStatementParameter(final AbstractSqlParameterBean parameterBean, final List<T> valueList) {
        this.parameterBean = parameterBean;
        this.valueList = null;
        this.valueIndex = 0;
        this.valueList = valueList;
    }

    protected final AbstractSqlParameterBean getParameterBean() {
        return parameterBean;
    }

    public final int getOrder() {
        return parameterBean.getOrder();
    }

    public final int getIterations() {
        return parameterBean.getIterations();
    }

    /**
     * Returns the next value to use within this parameter. If the parameter supports multiple values, each
     * invocation of this method will return a different value using a Round-Robin algorithm.
     *
     * @return returns the next value to use within this parameter.
     */
    public synchronized T getValue(ExecutionContext context) throws IOException {
        if (valueList == null) {
            valueList = loadValues(context);
        }

        if (valueList.isEmpty()) {
            return null;
        }

        if (valueIndex >= valueList.size()) {
            valueIndex = 0;
        }

        return valueList.get(valueIndex++);
    }

    protected abstract List<T> loadValues(ExecutionContext context) throws IOException;
}
