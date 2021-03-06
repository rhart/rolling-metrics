/*
 *
 *  Copyright 2016 Vladimir Bukhtoyarov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.github.rollingmetrics.top;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.rollingmetrics.top.TestData.first;
import static com.github.rollingmetrics.top.TestData.second;
import static com.github.rollingmetrics.top.TestData.third;
import static com.github.rollingmetrics.top.impl.TopTestUtil.update;
import static org.junit.Assert.*;

public class TopMetricSetTest {

    private Top top = Top.builder(3).withSnapshotCachingDuration(Duration.ZERO).build();

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNullName() {
        new TopMetricSet(null, top, TimeUnit.MILLISECONDS, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowEmptyName() {
        new TopMetricSet("", top, TimeUnit.MILLISECONDS, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNullTop() {
        new TopMetricSet("my-top", null, TimeUnit.MILLISECONDS, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNullLatencyUnit() {
        new TopMetricSet("my-top", top, null, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldDisallowNegativeDigitsAfterDecimalPoint() {
        new TopMetricSet("my-top", top, TimeUnit.MILLISECONDS, -1);
    }

    @Test
    public void shouldAddLatencyUnitGauge() {
        for (TimeUnit timeUnit: TimeUnit.values()) {
            TopMetricSet metricSet = new TopMetricSet("my-top", top, timeUnit, 3);
            Map<String, Metric> metrics = metricSet.getMetrics();
            Gauge<String> timeUnitGauge = (Gauge<String>) metrics.get("my-top.latencyUnit");
            assertNotNull(timeUnitGauge);
            assertEquals(timeUnit.toString(), timeUnitGauge.getValue());
        }
    }

    @Test
    public void testDescriptionGauges() {
        TopMetricSet metricSet = new TopMetricSet("my-top", top, TimeUnit.MILLISECONDS, 3);
        checkDescriptions(metricSet, "my-top", "", "", "");

        update(top, first);
        checkDescriptions(metricSet, "my-top", first.getQueryDescription(), "", "");

        update(top, second);
        checkDescriptions(metricSet, "my-top", second.getQueryDescription(), first.getQueryDescription(), "");

        update(top, third);
        checkDescriptions(metricSet, "my-top", third.getQueryDescription(), second.getQueryDescription(), first.getQueryDescription());
    }

    @Test
    public void testValueGauges() {
        TopMetricSet metricSet = new TopMetricSet("my-top", top, TimeUnit.MILLISECONDS, 3);
        checkValues(metricSet, "my-top", 3, 0.0d, 0.0d, 0.0d);

        top.update(0, 13_345_456, TimeUnit.NANOSECONDS, () -> "SELECT * FROM USERS");
        checkValues(metricSet, "my-top", 3, 13.345d, 0.0d, 0.0d);


        top.update(0, 11_666_957, TimeUnit.NANOSECONDS, () -> "SELECT * FROM USERS");
        checkValues(metricSet, "my-top", 3, 13.345d, 11.666d, 0.0d);

        top.update(0, 2_004_123, TimeUnit.NANOSECONDS, () -> "SELECT * FROM DUAL");
        checkValues(metricSet, "my-top", 3, 13.345d, 11.666d, 2.004d);
    }

    private void checkDescriptions(TopMetricSet metricSet, String name, String... requiredDescriptions) {
        for (int i = 0; i < requiredDescriptions.length; i++) {
            String requiredDescription = requiredDescriptions[i];
            Gauge<String> gauge = (Gauge<String>) metricSet.getMetrics().get(name + "." + i + ".description");
            String description = gauge.getValue();
            assertEquals(requiredDescription, description);
        }
    }

    private void checkValues(TopMetricSet metricSet, String name, int digitsAfterDecimalPoint, double... requiredLatencies) {
        for (int i = 0; i < requiredLatencies.length; i++) {
            BigDecimal requiredLatency = new BigDecimal(requiredLatencies[i]).setScale(digitsAfterDecimalPoint, RoundingMode.CEILING);
            Gauge<BigDecimal> gauge = (Gauge<BigDecimal>) metricSet.getMetrics().get(name + "." + i + ".latency");
            BigDecimal latency = gauge.getValue();
            assertEquals(requiredLatency, latency);
        }
    }

}