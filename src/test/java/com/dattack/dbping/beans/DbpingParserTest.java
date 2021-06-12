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
package com.dattack.dbping.beans;

import com.dattack.jtoolbox.exceptions.DattackParserException;
import org.junit.jupiter.api.Test;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author cvarela
 * @since 0.1
 */
public class DbpingParserTest {

    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    @Test
    public void test() throws DattackParserException {

        final File file = new File("src/test/resources/simple-test.xml");

        final DbpingBean dbpingBean = DbpingParser.parse(file);
        assertEquals(1, dbpingBean.getTaskList().size(), "Unexpected size of task list");

        final PingTaskBean pingTaskBean = dbpingBean.getTaskList().get(0);

        assertAll("Should return a valid PingTaskBean",
            () -> assertNull(pingTaskBean.getCommandProvider(), "Unexpected command provider"),
            () -> assertEquals("jdbc/ds", pingTaskBean.getDatasource(), "Unexpected datasource"),
            () -> assertEquals("simple-test", pingTaskBean.getName(), "Unexpected task name"),
            () -> assertEquals(2, pingTaskBean.getThreads(), "Unexpected 'threads' value"),
            () -> assertEquals(10, pingTaskBean.getExecutions(), "Unexpected 'executions' value"),
            () -> assertEquals(1, pingTaskBean.getTimeBetweenExecutions(), "Unexpected 'timeBetweenExecutions' value"),
            () -> assertEquals(1, pingTaskBean.getSqlStatementList().size(), "Unexpected size of the statements list"),
            () -> assertEquals("./logs/dbping_${task.name}.log", pingTaskBean.getLogFile(), "Unexpected log file")
        );

        assertEquals("com.dattack.dbping.beans.SqlStatementBean",
            pingTaskBean.getSqlStatementList().get(0).getClass().getName(), "Unexpected command class");

        final SqlStatementBean sqlBean = (SqlStatementBean) pingTaskBean.getSqlStatementList().get(0);

        assertAll("Should return a valid SqlStatementBean",
            () -> assertEquals(1000, sqlBean.getBatchSize(), "Unexpected batch size"),
            () -> assertFalse(sqlBean.getContextBeanList().isEmpty(), "Is the context list empty?"),
            () -> assertAll("",
                () -> assertEquals("k1", sqlBean.getContextBeanList().get(0).getKey()),
                () -> assertEquals("v1", sqlBean.getContextBeanList().get(0).getValue()),
                () -> assertEquals("x", sqlBean.getContextBeanList().get(0).getActivation()),
                () -> assertEquals("-1", sqlBean.getContextBeanList().get(0).getUnset()),
                () -> assertEquals("k2", sqlBean.getContextBeanList().get(1).getKey()),
                () -> assertEquals("v2", sqlBean.getContextBeanList().get(1).getValue()),
                () -> assertEquals("y", sqlBean.getContextBeanList().get(1).getActivation()),
                () -> assertEquals("-2", sqlBean.getContextBeanList().get(1).getUnset())
            ),
            () -> assertEquals(100, sqlBean.getFetchSize(), "Unexpected fetch size"),
            () -> assertFalse(sqlBean.isIgnoreMetrics(), "Unexpected 'ignoreMetrics' value"),
            () -> assertEquals("sql-1", sqlBean.getLabel(), "Unexpected label"),
            () -> assertEquals(10, sqlBean.getMaxRowsToDump(), "Unexpected 'maxRowsToDump' value"),
            () -> assertEquals(2, sqlBean.getRepeats(), "Unexpected repeats value"),
            () -> assertEquals("SELECT * FROM salgrade WHERE hisal > 2500", sqlBean.getSql(), "Unexpected SQL code"),
            () -> assertEquals(0.3F, sqlBean.getWeight(), "Unexpected 'weight' value"),
            () -> assertFalse(sqlBean.isSkip(), "Unexpected 'skip' value"),
            () -> assertTrue(sqlBean.isUsePreparedStatement(), "Unexpected 'usePreparedStatement' value")
        );
    }
}
