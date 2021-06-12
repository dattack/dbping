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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dattack.dbping.log.LogEntry.LogEntryBuilder;
import com.dattack.formats.csv.CSVConfiguration;
import com.dattack.formats.csv.CSVObject;
import com.dattack.formats.csv.CSVReader;

/**
 * Class responsible for reading log files in CSV format.
 *
 * @author cvarela
 * @since 0.1
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CSVFileLogReader implements LogReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVFileLogReader.class);

    private final transient CSVReader reader;
    private final transient CSVConfiguration configuration;

    public CSVFileLogReader(final File dataFile) {
        configuration = new CSVConfigurationFactory().create();
        reader = new CSVReader(configuration, dataFile);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    // TODO: refactoring needed (PMD.AvoidSynchronizedAtMethodLevel)
    @Override
    public synchronized LogEntry next() throws IOException {

        while (true) {
            final CSVObject rawObject = reader.next();

            if (rawObject == null) {
                return null;
            }

            int index = 0;

            try {

                return new LogEntryBuilder() //
                        .withEventTime(configuration.getDateFormat().parse(rawObject.get(index++)).getTime()) //
                        .withTaskName(rawObject.get(index++)) //
                        .withThreadName(rawObject.get(index++)) //
                        .withIteration(Long.parseLong(rawObject.get(index++))) //
                        .withSqlLabel(rawObject.get(index++)) //
                        .withRows(Long.parseLong(rawObject.get(index++))) //
                        .withConnectionTime(Long.parseLong(rawObject.get(index++))) //
                        .withFirstRowTime(Long.parseLong(rawObject.get(index++))) //
                        .withTotalTime(Long.parseLong(rawObject.get(index))) //
                        .build();
            } catch (final ParseException e) {
                LOGGER.warn(e.getMessage());
            }
        }
    }
}
