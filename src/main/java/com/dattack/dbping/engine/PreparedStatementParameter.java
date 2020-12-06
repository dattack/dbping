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

import com.dattack.dbping.beans.SqlParameterBean;
import org.apache.commons.lang.StringUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A parameter that can be substituted in a PreparedStatement. If the parameter supports multiple values,each access
 * to the parameter will get a different value using a Round-Robin algorithm.
 *
 * @author cvarela
 * @since 0.2
 */
public class PreparedStatementParameter {

    private final SqlParameterBean parameterBean;
    private final List<String> valueList;
    private int index;

    /**
     * Default constructor.
     *
     * @param parameterBean the bean containing the configuration of the parameter
     * @throws IOException if an error occurs when accessing the values of the parameter
     */
    public PreparedStatementParameter(final SqlParameterBean parameterBean) throws IOException {
        this.parameterBean = parameterBean;
        this.valueList = new ArrayList<>();
        this.index = 0;
        loadValues();
    }

    protected final SqlParameterBean getParameterBean() {
        return parameterBean;
    }

    public final int getIndex() {
        return parameterBean.getIndex();
    }

    public final String getFormat() {
        return parameterBean.getFormat();
    }

    public final String getType() {
        return parameterBean.getType();
    }

    private void loadValues() throws IOException {

        if (StringUtils.isNotBlank(parameterBean.getFile())) {
            try (Stream<String> stream = Files.lines(Paths.get(getParameterBean().getFile()))) {
                stream.forEach(valueList::add);
            }
        } else if (StringUtils.isNotBlank(parameterBean.getValue())) {
            valueList.addAll(Arrays.stream(parameterBean.getValue().split(",")) //
                    .map(String::trim).collect(Collectors.toList()));
        }
    }

    /**
     * Returns the next value to use within this parameter. If the parameter supports multiple values, each
     * invocation of this method will return a different value using a Round-Robin algorithm.
     *
     * @return returns the next value to use within this parameter.
     */
    public synchronized String getValue() {
        if (valueList.isEmpty()) {
            return null;
        }

        if (index >= valueList.size()) {
            index = 0;
        }

        return valueList.get(index++);
    }
}
