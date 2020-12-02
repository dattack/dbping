/*
 * Copyright (c) 2015, The Dattack team (http://www.dattack.com)
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
package com.dattack.dbping.log;

import com.dattack.formats.csv.CSVConfiguration;
import com.dattack.formats.csv.CSVConfiguration.CsvConfigurationBuilder;

/**
 * @author cvarela
 * @since 0.1
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CSVConfigurationFactory {

    private static final String DEFAULT_SEPARATOR = "\t";

    private final String separator;

    public CSVConfigurationFactory() {
        this.separator = DEFAULT_SEPARATOR;
    }

    public CSVConfiguration create() {
        return new CsvConfigurationBuilder().withSeparator(separator).build();
    }
}
