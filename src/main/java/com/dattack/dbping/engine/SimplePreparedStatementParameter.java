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

import com.dattack.dbping.beans.SimpleSqlParameterBean;
import com.dattack.jtoolbox.commons.configuration.ConfigurationUtil;
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
public class SimplePreparedStatementParameter extends AbstractPreparedStatementParameter<String> {

    /**
     * Default constructor.
     *
     * @param parameterBean the bean containing the configuration of the parameter
     */
    public SimplePreparedStatementParameter(final SimpleSqlParameterBean parameterBean) {
        super(parameterBean);
    }

    public SimplePreparedStatementParameter(final SimpleSqlParameterBean parameterBean, final String... values) {
        super(parameterBean, Arrays.asList(values));
    }

    public SimpleSqlParameterBean getBean() {
        return (SimpleSqlParameterBean) super.getParameterBean();
    }

    public final String getFormat() {
        return getBean().getFormat();
    }

    public final int getRef() {
        return getBean().getRef();
    }

    public final String getType() {
        return getBean().getType();
    }

    protected List<String> loadValues(final ExecutionContext context) throws IOException {

        final List<String> valueList = new ArrayList<>();
        if (StringUtils.isNotBlank(getBean().getFile())) {
            try (Stream<String> stream = Files.lines(Paths.get(
                    ConfigurationUtil.interpolate(getBean().getFile(), context.getConfiguration())))) {
                stream.forEach(valueList::add);
            }
        } else if (StringUtils.isNotBlank(getBean().getValue())) {
            valueList.addAll(Arrays.stream(getBean().getValue().split(",")) //
                    .map(String::trim).collect(Collectors.toList()));
        }
        return valueList;
    }
}
