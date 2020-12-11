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
package com.dattack.dbping.engine;

import com.dattack.dbping.beans.DbpingBean;
import com.dattack.dbping.beans.DbpingParser;
import com.dattack.dbping.beans.PingTaskBean;
import com.dattack.dbping.log.CSVFileLogWriter;
import com.dattack.dbping.log.LogHeader;
import com.dattack.dbping.log.LogWriter;
import com.dattack.jtoolbox.commons.configuration.ConfigurationUtil;
import com.dattack.jtoolbox.exceptions.DattackParserException;
import com.dattack.jtoolbox.io.FilesystemUtils;
import com.dattack.jtoolbox.jdbc.JNDIDataSource;
import org.apache.commons.configuration.CompositeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.Set;
import javax.sql.DataSource;

/**
 * Execution engine responsible for starting {@link PingJob}.
 *
 * @author cvarela
 * @since 0.1
 */
public final class PingEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(PingEngine.class);

    private static SqlCommandProvider getCommandProvider(final String clazzname) {

        SqlCommandProvider sentenceProvider = null;

        if (clazzname != null) {
            try {
                sentenceProvider = (SqlCommandProvider) Class.forName(clazzname).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                LOGGER.trace(String.format("Using default SqlSentenceProvider: %s", e.getMessage()));
                // ignore
            }
        }

        if (sentenceProvider == null) {
            sentenceProvider = new SqlCommandRoundRobinProvider();
        }
        return sentenceProvider;
    }

    private void execute(final File file, final Set<String> taskNames) throws DattackParserException {

        if (file.isDirectory()) {

            final File[] files = file.listFiles(FilesystemUtils.createFilenameFilterByExtension("xml"));
            if (files != null) {
                for (final File child : files) {
                    execute(child, taskNames);
                }
            }

        } else {

            final DbpingBean dbpingBean = DbpingParser.parse(file);
            for (final PingTaskBean pingTaskBean : dbpingBean.getTaskList()) {

                if (taskNames != null && !taskNames.isEmpty() && !taskNames.contains(pingTaskBean.getName())) {
                    continue;
                }

                final CompositeConfiguration conf = new CompositeConfiguration();
                conf.setProperty("task.name", pingTaskBean.getName());
                conf.addConfiguration(ConfigurationUtil.createEnvSystemConfiguration());

                final DataSource dataSource = new JNDIDataSource(pingTaskBean.getDatasource());

                final SqlCommandProvider commandProvider = getCommandProvider(pingTaskBean.getCommandProvider());
                commandProvider.setSentences(pingTaskBean.getSqlStatementList());

                final LogWriter logWriter = new CSVFileLogWriter(
                        ConfigurationUtil.interpolate(pingTaskBean.getLogFile(), conf));

                final LogHeader logHeader = new LogHeader(pingTaskBean);
                logWriter.write(logHeader);

                if (commandProvider.getSize() > 0) {
                    for (int i = 0; i < pingTaskBean.getThreads(); i++) {
                        new Thread(new PingJob(pingTaskBean, dataSource, commandProvider, logWriter)).start();
                    }
                }
            }
        }
    }

    /**
     * Searches in the indicated files for those tasks whose name coincides with one of the indicated ones. If the
     * list of task names is empty then it will start all the tasks that are configured in the provided files.
     *
     * @param filenames paths in which to start the search. These paths can be files or directories.
     * @param taskNames the set of task names. It can be empty or null.
     * @throws DattackParserException when there is a problem reading any of the configuration files.
     */
    public void execute(final String[] filenames, final Set<String> taskNames)
            throws DattackParserException {

        for (final String filename : filenames) {
            execute(new File(filename), taskNames);
        }
    }
}
