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
package com.dattack.dbping.cli;

import com.dattack.dbping.beans.PingTaskBean;
import com.dattack.dbping.engine.PingEngine;
import com.dattack.dbping.engine.PingTaskSelector;
import com.dattack.jtoolbox.exceptions.DattackParserException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Starts the DBPing CLI tool.
 *
 * @author cvarela
 * @since 0.1
 */
@SuppressWarnings({"PMD.SystemPrintln", "PMD.AvoidPrintStackTrace"})
public final class PingCli {

    private static final String FILE_OPTION = "f";
    private static final String LONG_FILE_OPTION = "file";
    private static final String TASK_NAME_OPTION = "t";
    private static final String LONG_TASK_NAME_OPTION = "task";
    private static final String LIST_OPTION = "l";
    private static final String LONG_LIST_OPTION = "list";

    private PingCli() {
        // utility class
    }

    private static Options createOptions() {

        final Options options = new Options();

        options.addOption(Option.builder(FILE_OPTION) //
                .required(true) //
                .longOpt(LONG_FILE_OPTION) //
                .hasArg(true) //
                .argName("DBPING_FILE") //
                .desc("the path of the file containing the DBPing configuration") //
                .build());

        options.addOption(Option.builder(TASK_NAME_OPTION) //
                .required(false) //
                .longOpt(LONG_TASK_NAME_OPTION) //
                .hasArg(true) //
                .argName("TASK_NAME") //
                .desc("the name of the task to execute") //
                .build());

        options.addOption(Option.builder(LIST_OPTION) //
                .required(false) //
                .longOpt(LONG_LIST_OPTION) //
                .hasArg(false) //
                .desc("list the name of the tasks contained in the configuration file") //
                .build());

        return options;
    }

    private static void list(final String[] filenames, final Set<String> taskNames) {

        final PingTaskSelector selector = new PingTaskSelector();
        final Map<String, List<PingTaskBean>> map = selector.filter(filenames, taskNames);

        final List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);

        System.out.println("TASKS LIST");

        for (final String key : keys) {
            System.out.format("%n- %s%n", key);
            final List<PingTaskBean> tasks = map.get(key);
            Collections.sort(tasks, Comparator.comparing(PingTaskBean::getName));

            for (final PingTaskBean bean : tasks) {
                System.out.format("    - %s%n", bean.getName());
            }
        }
    }

    /**
     * The <code>main</code> method.
     *
     * @param args
     *            the program arguments
     */
    public static void main(final String[] args) {

        final Options options = createOptions();

        try {
            final CommandLineParser parser = new DefaultParser();
            final CommandLine cmd = parser.parse(options, args);
            final String[] filenames = cmd.getOptionValues(FILE_OPTION);
            final String[] taskNames = cmd.getOptionValues(TASK_NAME_OPTION);

            HashSet<String> hs = null;
            if (taskNames != null) {
                hs = new HashSet<>(Arrays.asList(taskNames));
            }

            if (cmd.hasOption(LIST_OPTION)) {
                list(filenames, hs);
            } else {
                final PingEngine ping = new PingEngine();
                ping.execute(filenames, hs);
            }

        } catch (@SuppressWarnings("unused") final ParseException e) {
            showUsage(options);
        } catch (final DattackParserException | IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void showUsage(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        final int descPadding = 5;
        final int leftPadding = 4;
        formatter.setDescPadding(descPadding);
        formatter.setLeftPadding(leftPadding);
        final String header = "\n";
        final String footer = "\nPlease report issues at https://github.com/dattack/dbtools/issues";
        formatter.printHelp("dbping ", header, options, footer, true);
    }
}
