/*
 *    Copyright 2017 Vladimir Bukhtoyarov
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

package com.github.rollingmetrics.top.impl;

import com.github.rollingmetrics.retention.RetentionPolicy;
import com.github.rollingmetrics.top.Top;
import com.github.rollingmetrics.top.TopBuilder;
import com.github.rollingmetrics.top.TopTestData;
import com.github.rollingmetrics.util.Ticker;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;

public class TopBuilderTest {

    @Test(expected = IllegalArgumentException.class)
    public void tooManyPositionsShouldBeDisallowed() {
        RetentionPolicy.uniform()
                .newTopBuilder(TopRecorderSettings.MAX_POSITION_COUNT + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyPositionsShouldBeDisallowed2() {
        RetentionPolicy.uniform()
                .newTopBuilder(1)
                .withPositionCount(TopRecorderSettings.MAX_POSITION_COUNT + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroPositionsShouldBeDisallowed() {
        RetentionPolicy.uniform()
                .newTopBuilder(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroPositionsShouldBeDisallowed2() {
        RetentionPolicy.uniform()
                .newTopBuilder(1)
                .withPositionCount(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullSlowQueryThresholdShouldBeDisallowed() {
        RetentionPolicy.uniform()
                .newTopBuilder(1)
                .withLatencyThreshold(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeSlowQueryThresholdShouldBeDisallowed() {
        RetentionPolicy.uniform()
                .newTopBuilder(1)
                .withLatencyThreshold(Duration.ofMillis(-1));
    }

    @Test
    public void zeroSlowQueryThresholdShouldBeAllowed() {
        RetentionPolicy.uniform()
                .newTopBuilder(1)
                .withLatencyThreshold(Duration.ZERO);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortQueryDescriptionLengthShouldBeDisallowed() {
        Top.builder(1).withMaxLengthOfQueryDescription(TopBuilder.MIN_LENGTH_OF_QUERY_DESCRIPTION - 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullIntervalBetweenResettingShouldBeDisallowed() {
        Top.builder(1).resetAllPositionsPeriodically(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeIntervalBetweenResettingShouldBeDisallowed() {
        Top.builder(1).resetAllPositionsPeriodically(Duration.ofSeconds(-60));
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortIntervalBetweenResettingShouldBeDisallowed() {
        Top.builder(1).resetAllPositionsPeriodically(Duration.ofMillis(MIN_CHUNK_RESETTING_INTERVAL_MILLIS - 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooManyChunksShouldBeDisallowed() {
        Top.builder(1).resetPositionsPeriodicallyByChunks(Duration.ofDays(1), TopBuilder.MAX_CHUNKS + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void lesserThanTwoChunksShouldBeDisallowed() {
        Top.builder(1).resetPositionsPeriodicallyByChunks(Duration.ofDays(1), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullRollingWindowShouldBeDisallowed() {
        Top.builder(1).resetPositionsPeriodicallyByChunks(null, TopBuilder.MAX_CHUNKS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooShortChunkTtlShouldBeDisallowed() {
        Top.builder(1).resetPositionsPeriodicallyByChunks(Duration.ofMillis(TopBuilder.MIN_CHUNK_RESETTING_INTERVAL_MILLIS * 10 - 1), 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeChunkTtlShouldBeDisallowed() {
        Top.builder(1).resetPositionsPeriodicallyByChunks(Duration.ofMillis(-2000), 2);
    }

    @Test
    public void cachingPeriodShouldBeApplied() {
        AtomicLong currentTimeMillis = new AtomicLong();
        Ticker ticker = Ticker.mock(currentTimeMillis);
        Top top = Top.builder(1)
                .neverResetPositions()
                .withTicker(ticker)
                .withSnapshotCachingDuration(Duration.ofSeconds(10))
                .build();

        assertEquals(1,top.getSize());

        TopTestUtil.update(top, TopTestData.first);
        TopTestUtil.checkOrder(top, TopTestData.first);

        TopTestUtil.update(top, TopTestData.second);
        TopTestUtil.checkOrder(top, TopTestData.first);

        currentTimeMillis.addAndGet(10_000);
        TopTestUtil.checkOrder(top, TopTestData.second);
    }

    @Test
    public void shouldUse1SecondCachingPeriodByDefault() {
        AtomicLong currentTimeMillis = new AtomicLong();
        Ticker ticker = Ticker.mock(currentTimeMillis);
        Top top = Top.builder(1)
                .neverResetPositions()
                .withTicker(ticker)
                .build();

        TopTestUtil.update(top, TopTestData.first);
        TopTestUtil.checkOrder(top, TopTestData.first);

        TopTestUtil.update(top, TopTestData.second);
        TopTestUtil.checkOrder(top, TopTestData.first);

        currentTimeMillis.addAndGet(1_000);
        TopTestUtil.checkOrder(top, TopTestData.second);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCachingDurationShouldBeDisallowed() {
        Top.builder(1).withSnapshotCachingDuration(Duration.ofMillis(-2000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCachingDurationShouldBeDisallowed() {
        Top.builder(1).withSnapshotCachingDuration(null);
    }

    @Test
    public void shouldAllowToReplaceSize() {
        Top top = Top.builder(1).withPositionCount(2).build();
        TopTestUtil.update(top, TopTestData.first);
        TopTestUtil.update(top, TopTestData.second);
        TopTestUtil.checkOrder(top, TopTestData.second, TopTestData.first);
    }

}