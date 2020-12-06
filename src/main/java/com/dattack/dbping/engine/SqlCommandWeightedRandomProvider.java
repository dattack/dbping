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
package com.dattack.dbping.engine;

import java.util.List;
import java.util.Random;

import com.dattack.dbping.beans.SqlCommandBean;

/**
 * Selects a query from the provided list using a weighted randon selection algorithm.
 *
 * @author cvarela
 * @since 0.1
 */
public class SqlCommandWeightedRandomProvider extends SqlCommandProvider {

    private final Random randomGenerator;
    private float[] cumulativeWeight;

    private static float norm(final float weight, final float sumWeight) {
        return weight / sumWeight;
    }

    public SqlCommandWeightedRandomProvider() {
        this.randomGenerator = new Random();
    }

    // the cumulative density function
    private float[] cdf(List<SqlCommandBean> sentenceList) {

        float totalWeight = 0;
        for (final SqlCommandBean sentence : sentenceList) {
            totalWeight += sentence.getWeight();
        }

        final float[] cdfValues = new float[sentenceList.size()];
        for (int i = 0; i < cdfValues.length; i++) {
            final float normWeight = norm(sentenceList.get(i).getWeight(), totalWeight);
            if (i == 0) {
                cdfValues[i] = normWeight;
            } else {
                cdfValues[i] = cdfValues[i - 1] + normWeight;
            }
        }

        return cdfValues;
    }

    @Override
    protected void prepare(List<SqlCommandBean> sqlList) {
        this.cumulativeWeight = cdf(sqlList);
    }

    @Override
    public ExecutableCommand nextSql() {

        if (isEmpty()) {
            throw new IllegalArgumentException("The sentence list must not be null or empty");
        }

        final float randomWeight = randomGenerator.nextFloat();
        for (int i = 0; i < cumulativeWeight.length; i++) {
            if (cumulativeWeight[i] > randomWeight) {
                return getCommand(i);
            }
        }
        return getCommand(getSize() - 1);
    }
}
