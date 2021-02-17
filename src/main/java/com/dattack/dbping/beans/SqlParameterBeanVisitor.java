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
package com.dattack.dbping.beans;

/**
 * Visitor pattern for the SqlCommandBean class hierarchy.
 *
 * @author cvarela
 * @since 0.2
 */
public interface SqlParameterBeanVisitor<T extends Throwable> {

    void visit(final SimpleSqlParameterBean bean) throws T;

    void visit(final ClusterSqlParameterBean bean) throws T;
}
