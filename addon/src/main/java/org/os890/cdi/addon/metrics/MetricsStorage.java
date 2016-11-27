/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.os890.cdi.addon.metrics;

import org.os890.cdi.addon.circuitbreaker.api.CircuitEvent;
import org.os890.cdi.addon.circuitbreaker.api.CircuitState;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class MetricsStorage {
    private long configuredMaxDuration = 50; //TODO read it from the config
    private AtomicLong OVERALL_SLOW_CALLS = new AtomicLong(0);
    private AtomicLong OVERALL_FAST_CALLS = new AtomicLong(0);

    private Map<String, MetricsEntry> entries = new ConcurrentHashMap<String, MetricsEntry>();

    public void record(String key, long duration) {
        try {
            long newCallCount;
            if (duration > configuredMaxDuration) {
                MetricsEntry entry = getOrCreateEntry(key);

                entry.recordSlowCall(duration);
                newCallCount = OVERALL_SLOW_CALLS.incrementAndGet();
                if (newCallCount < 0) {
                    resetOveralCallStats();
                }
            } else {
                newCallCount = OVERALL_FAST_CALLS.incrementAndGet();
                if (newCallCount < 0) {
                    resetOveralCallStats();
                }
            }

        } catch (Throwable t) {
            //don't handle exceptions during metrics-handling
        }
    }

    public double calcPercentageOfSlowCalls() {
        long slowCalls = OVERALL_SLOW_CALLS.get();
        long fastCalls = OVERALL_FAST_CALLS.get();

        if (slowCalls == 0L) {
            return 0L;
        }
        return new BigDecimal(100).divide(new BigDecimal(slowCalls + fastCalls), 2, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(slowCalls)).longValue();
    }

    public Map<String, Long> calcOverallAverage() {
        Map<String, Long> result = new HashMap<String, Long>();
        for (Map.Entry<String, MetricsEntry> entry : entries.entrySet()) {
            MetricsEntry metricsEntry = entry.getValue();

            if (metricsEntry == null) {
                continue;
            }

            double duration = 0L;
            long numberOfEntries = 0L;
            for (StatsEntry statsEntry : metricsEntry.getStatsEntries()) {
                duration += statsEntry.getAverageDuration();
                numberOfEntries++; //don't use the number of calls (it's used for the average-calc)
            }

            if (numberOfEntries > 0L) {
                result.put(entry.getKey(), new BigDecimal(duration).divide(new BigDecimal(numberOfEntries), BigDecimal.ROUND_HALF_UP).longValue());
            }
        }

        return result;
    }

    public Map<String, Long> calcOverallMin() {
        Map<String, Long> result = new HashMap<String, Long>();
        for (Map.Entry<String, MetricsEntry> entry : entries.entrySet()) {
            MetricsEntry metricsEntry = entry.getValue();

            if (metricsEntry == null) {
                continue;
            }

            long currentMin;
            long globalMin = Long.MAX_VALUE;
            for (StatsEntry statsEntry : metricsEntry.getStatsEntries()) {
                currentMin = statsEntry.getMinDuration();
                if (currentMin < globalMin) {
                    globalMin = currentMin;
                }
            }

            result.put(entry.getKey(), globalMin);
        }
        return result;
    }

    public Map<String, Long> calcOverallMax() {
        Map<String, Long> result = new HashMap<String, Long>();
        for (Map.Entry<String, MetricsEntry> entry : entries.entrySet()) {
            MetricsEntry metricsEntry = entry.getValue();

            if (metricsEntry == null) {
                continue;
            }

            long currentMax;
            long globalMax = 0L;
            for (StatsEntry statsEntry : metricsEntry.getStatsEntries()) {
                currentMax = statsEntry.getMaxDuration();
                if (currentMax > globalMax) {
                    globalMax = currentMax;
                }
            }

            result.put(entry.getKey(), globalMax);
        }
        return result;
    }

    public Map<String, Long> calcPercentile(double percentage) {
        if (percentage > 0.99) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new HashMap<String, Long>();
        for (Map.Entry<String, MetricsEntry> entry : entries.entrySet()) {
            MetricsEntry metricsEntry = entry.getValue();

            if (metricsEntry == null) {
                continue;
            }

            List<Long> averageListAcrossTimeslots = new ArrayList<Long>();
            for (StatsEntry statsEntry : metricsEntry.getStatsEntries()) {
                averageListAcrossTimeslots.add(new Double(statsEntry.getAverageDuration()).longValue());
            }

            if (averageListAcrossTimeslots.isEmpty()) {
                continue;
            }

            Collections.sort(averageListAcrossTimeslots);

            int index = new BigDecimal(averageListAcrossTimeslots.size()).multiply(new BigDecimal(percentage)).intValue();
            averageListAcrossTimeslots = averageListAcrossTimeslots.subList(0, index);
            double duration = 0L;
            for (Long currentDuration : averageListAcrossTimeslots) {
                duration += currentDuration;
            }
            result.put(entry.getKey(), new BigDecimal(duration).divide(new BigDecimal(averageListAcrossTimeslots.size()), BigDecimal.ROUND_HALF_UP).longValue());
        }
        return result;
    }

    private MetricsEntry getOrCreateEntry(String key) {
        MetricsEntry entry = entries.get(key);

        if (entry == null) {
            entry = buildEntry(key);
        }
        return entry;
    }

    public void onOpenCircuit(@Observes @CircuitState(CircuitState.Value.OPEN) CircuitEvent circuitEvent) {
        MetricsEntry entry = getOrCreateEntry(circuitEvent.getMethodKey());
        entry.onOpenCircuit();
    }

    public void onHalfOpenCircuit(@Observes @CircuitState(CircuitState.Value.HALF_OPEN) CircuitEvent circuitEvent) {
        MetricsEntry entry = getOrCreateEntry(circuitEvent.getMethodKey());
        entry.onHalfOpenCircuit();
    }

    public void onCloseCircuit(@Observes @CircuitState(CircuitState.Value.CLOSED) CircuitEvent circuitEvent) {
        MetricsEntry entry = getOrCreateEntry(circuitEvent.getMethodKey());
        entry.onCloseCircuit();
    }

    private synchronized MetricsEntry buildEntry(String key) {
        MetricsEntry result = entries.get(key);

        if (result != null) {
            return result;
        }
        result = new MetricsEntry();
        entries.put(key, result);
        return result;
    }

    private void resetOveralCallStats() {
        OVERALL_SLOW_CALLS = new AtomicLong(0);
        OVERALL_FAST_CALLS = new AtomicLong(0);
    }
}
