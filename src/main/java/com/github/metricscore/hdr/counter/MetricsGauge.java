/*
 *    Copyright 2016 Vladimir Bukhtoyarov
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.github.metricscore.hdr.counter;

import com.codahale.metrics.Gauge;

import java.util.Objects;

/**
 * This is adapter for any implementation of {@link WindowCounter} which convert it to {@link Gauge}.
 * The windowCounter wrapped by this adapter can be added to {@link com.codahale.metrics.MetricRegistry} as {@link Gauge}.
 *
 * <p><br> The example of usage:
 * <pre><code>
 *         WindowCounter counter = new ResetAtSnapshotCounter();
 *         registry.register("my-gauge", new MetricsGauge(counter));
 *     </code>
 * </pre>
 *
 */
public class MetricsGauge implements Gauge<Long> {

    private final WindowCounter counter;

    public MetricsGauge(WindowCounter counter) {
        this.counter = Objects.requireNonNull(counter);
    }

    @Override
    public Long getValue() {
        return counter.getSum();
    }

}