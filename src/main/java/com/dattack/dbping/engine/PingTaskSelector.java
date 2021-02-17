/*
 * Copyright (c) 2021, The Dattack team (http://www.dattack.com)
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
import com.dattack.jtoolbox.util.CollectionUtils;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;

/**
 * Execution engine responsible for starting {@link PingJob}.
 *
 * @author cvarela
 * @since 0.1
 */
public final class PingTaskSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(PingEngine.class);

    private HashMap<String, List<PingTaskBean>> filter(final File file, final Set<String> taskNames) {

        HashMap<String, List<PingTaskBean>> map = new HashMap<>();
        if (file.isDirectory()) {

            final File[] files = file.listFiles(FilesystemUtils.createFilenameFilterByExtension("xml"));
            if (files != null) {
                for (final File child : files) {
                    map.putAll(filter(child, taskNames));
                }
            }

        } else {

            try {
                final DbpingBean dbpingBean = DbpingParser.parse(file);
                List<PingTaskBean> list = new ArrayList<>();
                map.put(file.toString(), list);
                for (final PingTaskBean pingTaskBean : dbpingBean.getTaskList()) {

                    if (CollectionUtils.isNotEmpty(taskNames)
                            && taskNames.stream().noneMatch(pingTaskBean.getName()::equalsIgnoreCase)) {
                        continue;
                    }
                    list.add(pingTaskBean);
                }
            } catch (DattackParserException e) {
                LOGGER.warn(e.getMessage());
            }

        }
        return map;
    }

    /**
     * Searches in the indicated files for those tasks whose name coincides with one of the indicated ones. If the
     * list of task names is empty then it will select all the tasks that are configured in the provided files.
     *
     * @param filenames paths in which to start the search. These paths can be files or directories.
     * @param taskNames the set of task names. It can be empty or null.
     */
    public HashMap<String, List<PingTaskBean>> filter(final String[] filenames, final Set<String> taskNames) {

        HashMap<String, List<PingTaskBean>> map = new HashMap<>();
        for (final String filename : filenames) {
            map.putAll(filter(new File(filename), taskNames));
        }
        return map;
    }
}
