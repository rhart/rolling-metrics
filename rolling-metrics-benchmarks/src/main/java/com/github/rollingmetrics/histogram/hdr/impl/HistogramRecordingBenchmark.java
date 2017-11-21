/*
 *
 *  Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.histogram.hdr.impl;

import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Histogram;
import com.github.rollingmetrics.histogram.OverflowResolver;
import com.github.rollingmetrics.histogram.hdr.RollingHdrHistogram;
import com.github.rollingmetrics.retention.RetentionPolicy;
import com.github.rollingmetrics.util.BackgroundTicker;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class HistogramRecordingBenchmark {

    @State(Scope.Benchmark)
    public static class HistogramState {

        BackgroundTicker backgroundClock;

        final RollingHdrHistogram chunkedHistogram = RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                .newRollingHdrHistogramBuilder()
                .build()
                ;

        final RollingHdrHistogram upperLimitedChunkedHistogram = RetentionPolicy
                .resetPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                .newRollingHdrHistogramBuilder()
                .withLowestDiscernibleValue(TimeUnit.MICROSECONDS.toNanos(1))
                .withHighestTrackableValue(TimeUnit.MINUTES.toNanos(5), OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .build();

        final RollingHdrHistogram resetOnSnapshotHistogram = RetentionPolicy
                .resetOnSnapshot()
                .newRollingHdrHistogramBuilder()
                .withLowestDiscernibleValue(TimeUnit.MICROSECONDS.toNanos(1))
                .withHighestTrackableValue(TimeUnit.MINUTES.toNanos(5), OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .build();

        final RollingHdrHistogram resetPeriodicallyHistogram = RetentionPolicy
                .resetPeriodically(Duration.ofSeconds(300))
                .newRollingHdrHistogramBuilder()
                .withLowestDiscernibleValue(TimeUnit.MICROSECONDS.toNanos(1))
                .withHighestTrackableValue(TimeUnit.MINUTES.toNanos(5), OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .build();

        final RollingHdrHistogram uniformHistogram = RetentionPolicy
                .uniform()
                .newRollingHdrHistogramBuilder()
                .withLowestDiscernibleValue(TimeUnit.MICROSECONDS.toNanos(1))
                .withHighestTrackableValue(TimeUnit.MINUTES.toNanos(5), OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                .build();

        final Histogram metricsCoreHistogram = new Histogram(new ExponentiallyDecayingReservoir());

        private RollingHdrHistogram chunkedHistogramWithBackgroundClock;
        private RollingHdrHistogram upperLimitedChunkedHistogramWithBackgroundClock;
        private RollingHdrHistogram resetPeriodicallyHistogramWithBackgroundClock;

        @Setup
        public void setup() {
            backgroundClock = new BackgroundTicker(100);

            this.chunkedHistogramWithBackgroundClock = RetentionPolicy
                    .resetPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                    .newRollingHdrHistogramBuilder()
                    .withTicker(backgroundClock)
                    .build();

            this.upperLimitedChunkedHistogramWithBackgroundClock = RetentionPolicy
                    .resetPeriodicallyByChunks(Duration.ofSeconds(3), 3)
                    .newRollingHdrHistogramBuilder()
                    .withTicker(backgroundClock)
                    .withLowestDiscernibleValue(TimeUnit.MICROSECONDS.toNanos(1))
                    .withHighestTrackableValue(TimeUnit.MINUTES.toNanos(5), OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                    .build();

            this.resetPeriodicallyHistogramWithBackgroundClock = RetentionPolicy
                    .resetPeriodically(Duration.ofSeconds(300))
                    .newRollingHdrHistogramBuilder()
                    .withTicker(backgroundClock)
                    .withLowestDiscernibleValue(TimeUnit.MICROSECONDS.toNanos(1))
                    .withHighestTrackableValue(TimeUnit.MINUTES.toNanos(5), OverflowResolver.REDUCE_TO_HIGHEST_TRACKABLE)
                    .build();
        }

        @TearDown
        public void tearDown() {
            backgroundClock.stop();
        }

    }

    @Benchmark
    public long baseLine() {
        return getRandomValue();
    }

    @Benchmark
    public long baseLineGetCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Benchmark
    public void updateMetricsCoreHistogram(HistogramState state) {
        state.metricsCoreHistogram.update(getRandomValue());
    }

    @Benchmark
    public void updateUniformHistogram(HistogramState state) {
        state.uniformHistogram.update(getRandomValue());
    }

    @Benchmark
    public void updateResetPeriodicallyHistogramWithBackgroundClock(HistogramState state) {
        state.resetPeriodicallyHistogramWithBackgroundClock.update(getRandomValue());
    }

    @Benchmark
    public void updateChunkedHistogramWithBackgroundClock(HistogramState state) {
        state.chunkedHistogramWithBackgroundClock.update(getRandomValue());
    }

    @Benchmark
    public void updateChunkedUpperLimitedHistogramWithBackgroundClock(HistogramState state) {
        state.upperLimitedChunkedHistogramWithBackgroundClock.update(getRandomValue());
    }

    @Benchmark
    public void updateResetPeriodicallyHistogram(HistogramState state) {
        state.resetPeriodicallyHistogram.update(getRandomValue());
    }

    @Benchmark
    public void updateResetOnSnapshotHistogram(HistogramState state) {
        state.resetOnSnapshotHistogram.update(getRandomValue());
    }

    @Benchmark
    public void updateChunkedHistogram(HistogramState state) {
        state.chunkedHistogram.update(getRandomValue());
    }

    @Benchmark
    public void updateChunkedUpperLimitedHistogram(HistogramState state) {
        state.upperLimitedChunkedHistogram.update(getRandomValue());
    }

    private static long getRandomValue() {
        return ThreadLocalRandom.current().nextLong(15_000_000) + 5_000_000;
    }

    public static class OneThread {
        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include(((Class) HistogramRecordingBenchmark.class).getSimpleName())
                    .warmupIterations(5)
                    .measurementIterations(5)
                    .threads(1)
                    .forks(1)
                    .build();
            try {
                new Runner(opt).run();
            } catch (RunnerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class FourThread {
        public static void main(String[] args) throws RunnerException {
            Options opt = new OptionsBuilder()
                    .include(((Class) HistogramRecordingBenchmark.class).getSimpleName())
                    .warmupIterations(5)
                    .measurementIterations(5)
                    .threads(4)
                    .forks(1)
                    .build();
            try {
                new Runner(opt).run();
            } catch (RunnerException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
