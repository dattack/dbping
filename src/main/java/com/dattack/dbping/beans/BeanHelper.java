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

import com.dattack.jtoolbox.commons.configuration.ConfigurationUtil;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.lang.StringUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author cvarela
 * @since 0.1
 */
public final class BeanHelper {

    private static final String FILE_PROTOCOL = "file://";

    private BeanHelper() {
        // static class
    }

    public static String getPlainSql(final String sql, final AbstractConfiguration configuration) throws IOException {

        String code = ConfigurationUtil.interpolate(sql, configuration);

        if (StringUtils.startsWithIgnoreCase(StringUtils.trimToEmpty(code), FILE_PROTOCOL)) {
            final String path = ConfigurationUtil.interpolate(sql.trim().substring(FILE_PROTOCOL.length()),
                    configuration);
            code = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);

            // interpolate SQL code
            code = ConfigurationUtil.interpolate(code, configuration);
        }

        return normalizeToEmpty(code);
    }

    public static String normalizeToEmpty(final String sql) {
        if (Objects.isNull(sql)) {
            return "";
        }
        return StringUtils.trimToEmpty(sql.replaceAll("\\s+", " "));
    }
}
