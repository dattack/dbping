/*
 * Copyright (c) 2014, The Dattack team (http://www.dattack.com)
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

import com.dattack.dbping.beans.BeanHelper;
import com.dattack.dbping.beans.SqlCommandBean;
import com.dattack.dbping.beans.SqlCommandVisitor;
import com.dattack.dbping.beans.SqlScriptBean;
import com.dattack.dbping.beans.SqlStatementBean;
import com.dattack.dbping.engine.DataRow;
import com.dattack.dbping.engine.ExecutionContext;
import com.dattack.dbping.engine.LogEntry;
import com.dattack.formats.csv.CSVStringBuilder;
import com.dattack.jtoolbox.commons.configuration.ConfigurationUtil;
import com.dattack.jtoolbox.io.IOUtils;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Class responsible for writing the collected metrics into the log file.
 *
 * @author cvarela
 * @since 0.1
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CSVFileLogWriter implements LogWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSVFileLogWriter.class);

    private static final String DATE_FORMAT = "%-30s";
    private String taskNameFormat = "%s";
    private String threadNameFormat = "%s";
    private String labelFormat = "%s";

    private final CSVStringBuilder csvBuilder;
    private final String filename;

    public CSVFileLogWriter(final String filename) {
        this.filename = filename;
        this.csvBuilder = new CSVStringBuilder(new CSVConfigurationFactory().create());
    }

    private void addDataRowList(final List<DataRow> list) {
        for (int i = 0; i < list.size(); i++) {
            final DataRow row = list.get(i);
            csvBuilder.comment().append(String.format(" Row %d:\t", i));
            for (final Object obj : row.getData()) {
                csvBuilder.append(ObjectUtils.toString(obj));
            }
            csvBuilder.eol();
        }
    }

    private String format(final LogEntry entry) {

        String data;
        synchronized (csvBuilder) {
            csvBuilder.append(new Date(entry.getEventTime())) //
                    .append(StringUtils.trimToEmpty(entry.getTaskName()), taskNameFormat) //
                    .append(StringUtils.trimToEmpty(entry.getThreadName()), threadNameFormat) //
                    .append(entry.getIteration(), "%5d") //
                    .append(StringUtils.trimToEmpty(entry.getSqlLabel()), labelFormat) //
                    .append(entry.getRows(), "%8d") //
                    .append(entry.getConnectionTime(), "%9d") //
                    .append(entry.getFirstRowTime(), "%9d") //
                    .append(entry.getTotalTime(), "%10d");

            if (entry.getException() != null) {
                csvBuilder.append(BeanHelper.normalize(entry.getException().getMessage()));
            } else {
                csvBuilder.append((String) null);
            }

            if (entry.getComment() != null) {
                csvBuilder.comment(BeanHelper.normalize(entry.getComment()), false, false);
            }

            csvBuilder.eol();
            addDataRowList(entry.getRowList());

            data = csvBuilder.toString();
            csvBuilder.clear();
        }
        return data;
    }

    private String format(final LogHeader header) {

        String data;
        synchronized (csvBuilder) {

            csvBuilder.comment();

            final List<String> keys = new ArrayList<>(header.getProperties().keySet());
            Collections.sort(keys);

            for (final String key : keys) {
                csvBuilder.comment(" " + BeanHelper.normalize(ObjectUtils.toString(key)) + ": " + //
                        BeanHelper.normalize(ObjectUtils.toString(header.getProperties().get(key))));
            }

            csvBuilder.comment(" DataSource: " + header.getPingTaskBean().getDatasource());

            int labelLength = 0;
            csvBuilder.comment(" SQL Sentences:");

            BaseConfiguration baseConfiguration = new BaseConfiguration();
            baseConfiguration.setProperty(ExecutionContext.PARENT_NAME_PROPERTY, header.getPingTaskBean().getName());

            CompositeConfiguration configuration = new CompositeConfiguration();
            configuration.addConfiguration(ConfigurationUtil.createEnvSystemConfiguration());
            configuration.addConfiguration(baseConfiguration);

            for (final SqlCommandBean sentence : header.getPingTaskBean().getSqlStatementList()) {

                labelLength = Math.max(labelLength, sentence.getLabel().length());

                sentence.accept(new SqlCommandVisitor() {

                    @Override
                    public void visit(final SqlScriptBean command) {
                        String commandLabel = ConfigurationUtil.interpolate(command.getLabel(), configuration);
                        csvBuilder.comment("  - " + commandLabel + ": ");

                        baseConfiguration.setProperty(ExecutionContext.PARENT_NAME_PROPERTY, commandLabel);

                        for (final SqlStatementBean item : command.getStatementList()) {
                            addComment(item, true);
                        }
                    }

                    @Override
                    public void visit(final SqlStatementBean command) {
                        addComment(command, false);
                    }

                    private void addComment(final SqlStatementBean item, boolean insideScript) {
                        String sql;
                        try {
                            sql = BeanHelper.getPlainSql(item.getSql(), configuration);
                        } catch (IOException e) {
                            sql = e.getMessage();
                        }

                        String pattern = insideScript ? "    |-- %s: %s" : "  - %s: %s";
                        csvBuilder.comment(String.format(pattern,
                                ConfigurationUtil.interpolate(item.getLabel(), configuration), sql));
                    }
                });
            }

            taskNameFormat = "%-" + header.getPingTaskBean().getName().length() + "s";
            threadNameFormat = "%-" + (header.getPingTaskBean().getName().length() + "@Thread-X".length() + 2) + "s";
            labelFormat = "%-" + Math.max(5, labelLength + 2) + "s";

            csvBuilder.comment() //
                    .append(" date", DATE_FORMAT) //
                    .append("task  ", taskNameFormat) //
                    .append("thread  ", threadNameFormat) //
                    .append("loop", "%5s") //
                    .append("label", labelFormat) //
                    .append("rows", "%8s") //
                    .append("conn-time", "%9s") //
                    .append("1row-time", "%9s") //
                    .append("total-time", "%10s") //
                    .append("message").eol();

            data = csvBuilder.toString();
            csvBuilder.clear();
        }
        return data;
    }

    private FileOutputStream getOutputStream() throws FileNotFoundException {

        final File file = new File(filename);
        if (!file.exists()) {
            final File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                LOGGER.warn("Unable to create directory: {}", parent);
            }
        }
        return new FileOutputStream(file, true);
    }

    @Override
    public void write(final LogEntry logEntry) {
        write(format(logEntry));
    }

    @Override
    public void write(final LogHeader logHeader) {
        write(format(logHeader));
    }

    private synchronized void write(final String message) {

        FileOutputStream out = null;
        try {
            out = getOutputStream();
            out.write(message.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            LOGGER.warn(e.getMessage());
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}
