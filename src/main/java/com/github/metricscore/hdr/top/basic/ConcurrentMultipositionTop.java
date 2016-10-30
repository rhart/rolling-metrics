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

package com.github.metricscore.hdr.top.basic;
import com.github.metricscore.hdr.top.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Is not a part of public API, this class just used as building block for other QueryTop implementations.
 *
 * This implementation supports concurrent updates, but top calculation is weakly consistent(inherited from {@link ConcurrentSkipListMap}),
 * so if weakly consistency is not enough then clients of this class should provide synchronization between reader and writers by itself.
 *
 */
public class ConcurrentMultipositionTop extends BaseTop implements ComposableTop<ConcurrentMultipositionTop> {

    private final ConcurrentSkipListMap<PositionKey, Position> top;
    private final AtomicLong phaseSequence = new AtomicLong();

    public ConcurrentMultipositionTop(int size, long slowQueryThresholdNanos, int maxLengthOfQueryDescription) {
        super(size, slowQueryThresholdNanos, maxLengthOfQueryDescription);
        this.top = new ConcurrentSkipListMap<>();

        // init by fake values
        long phase = phaseSequence.get();
        for (int i = 1; i <= size; i++) {
            top.put(new PositionKey(phase, -i), FAKE_QUERY);
        }
    }

    @Override
    protected void updateImpl(long latencyTime, TimeUnit latencyUnit, Supplier<String> descriptionSupplier, long latencyNanos) {
        PositionKey firstKey = top.firstKey();
        long currentPhase = phaseSequence.get();
        if (firstKey.latencyNanos >= latencyTime && firstKey.phase == currentPhase) {
            // the measure should be skipped because it is lesser then smallest which already tracked in the top.
            return;
        }
        String queryDescription = combineDescriptionWithLatency(latencyTime, latencyUnit, descriptionSupplier);
        Position position = new Position(latencyTime, latencyUnit, queryDescription);
        addLatency(currentPhase, latencyTime, position);
    }

    @Override
    public List<Position> getPositionsInDescendingOrder() {
        List<Position> descendingTop = new ArrayList<>(size);
        long currentPhase = phaseSequence.get();
        while (true) {
            for (Map.Entry<PositionKey, Position> entry : top.descendingMap().entrySet()) {
                PositionKey key = entry.getKey();
                Position position = key.phase < currentPhase? FAKE_QUERY: entry.getValue();
                descendingTop.add(position);
            }
            long phaseOnComplete = phaseSequence.get();
            if (phaseOnComplete == currentPhase) {
                return descendingTop;
            } else {
                currentPhase = phaseOnComplete;
                descendingTop.clear();
            }
        }
    }

    @Override
    public void reset() {
        // increasing phase will invalidate all recorded values for rating calculation,
        // so touching ConcurrentSkipListMap is not needed
        phaseSequence.incrementAndGet();
    }

    @Override
    public void addSelfToOther(ConcurrentMultipositionTop other) {
        long otherPhase = other.phaseSequence.get();
        long currentPhase = this.phaseSequence.get();
        for(Map.Entry<PositionKey, Position> otherEntry: other.top.descendingMap().entrySet()) {
            PositionKey otherKey = otherEntry.getKey();
            if (otherKey.phase != otherPhase) {
                return;
            }
            PositionKey firstKey = top.firstKey();
            if (firstKey.latencyNanos >= otherKey.latencyNanos && firstKey.phase == currentPhase) {
                return;
            }
            addLatency(currentPhase, otherKey.latencyNanos, otherEntry.getValue());
        }
    }

    @Override
    public ConcurrentMultipositionTop createNonConcurrentEmptyCopy() {
        return new ConcurrentMultipositionTop(size, slowQueryThresholdNanos);
    }

    private void addLatency(long phase, long latencyTime, Position position) {
        top.put(new PositionKey(phase, latencyTime), position);
        top.pollFirstEntry();
    }

    private static final class PositionKey implements Comparable<PositionKey> {
        final long phase;
        final long latencyNanos;

        public PositionKey(long phase, long latencyNanos) {
            this.phase = phase;
            this.latencyNanos = latencyNanos;
        }

        @Override
        public int compareTo(PositionKey other) {
            if (phase == other.phase) {
                return Long.compare(latencyNanos, other.latencyNanos);
            }
            // To compare two phase values f1 and f2 we should use f2 - f1 > 0, not f2 > f1,
            // because of the possibility of numerical overflow of AtomicLong.
            if (phase - other.phase > 0) {
                return 1;
            } else {
                return -1;
            }
        }
    }

}
