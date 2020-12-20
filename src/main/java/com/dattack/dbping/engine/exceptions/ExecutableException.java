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
package com.dattack.dbping.engine.exceptions;

import org.apache.commons.lang.exception.NestableException;

/**
 * @author cvarela
 * @since 0.4
 */
public class ExecutableException extends NestableException {

    private static final long serialVersionUID = -4102634848673853593L;

    private final String taskName;
    private final String sqlLabel;
    private final String sqlCode;

    public ExecutableException(final String taskName, final String sqlLabel, final String sqlCode, final Exception e) {
        super("Error (" + e.getMessage() + ") executing query: " + sqlCode, e);
        this.taskName = taskName;
        this.sqlLabel = sqlLabel;
        this.sqlCode = sqlCode;
    }

    public String getSqlCode() {
        return sqlCode;
    }

    public String getSqlLabel() {
        return sqlLabel;
    }

    public String getTaskName() {
        return taskName;
    }

    @Override
    public String toString() {
        return "ExecutableException{" +
                "message='" + super.getMessage() + '\'' +
                ", taskName='" + taskName + '\'' +
                ", sqlLabel='" + sqlLabel + '\'' +
                ", sqlCode='" + sqlCode + '\'' +
                "} " + super.toString();
    }
}
