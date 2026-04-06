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

package org.os890.cdi.addon.metrics.impl;

import org.os890.cdi.addon.circuitbreaker.api.CircuitEvent;
import org.os890.cdi.addon.circuitbreaker.api.CircuitState;
import org.os890.cdi.addon.metrics.api.FilterMethodsFasterThan;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Application-scoped storage for circuit-breaker invocation metrics.
 *
 * <p>Records call durations per method key, tracks circuit-breaker state
 * changes, and provides statistical calculations such as averages,
 * min/max values, and percentiles.</p>
 */
@ApplicationScoped
public class MetricsStorage {

    private AtomicLong overallSlowCalls = new AtomicLong(0);
    private AtomicLong overallFastCalls = new AtomicLong(0);

    private Map<String, MetricsEntry> entries = new ConcurrentHashMap<>();

    /**
     * Records a method call with the given duration.
     *
     * @param key           the method key
     * @param currentMethod the method that was called
     * @param duration      the call duration in milliseconds
     */
    public void record(String key, Method currentMethod, long duration) {
        try {
            long newCallCount;

            FilterMethodsFasterThan filterMethodsFasterThan = currentMethod.getAnnotation(FilterMethodsFasterThan.class);
            if (filterMethodsFasterThan == null) {
                filterMethodsFasterThan = FilterMethodsFasterThan.DEFAULT;
            }

            int filterMethodsFasterThanMs = filterMethodsFasterThan.ms();
            if (filterMethodsFasterThanMs < 0) {
                return;
            }

            if (duration >= filterMethodsFasterThanMs) {
                MetricsEntry entry = getOrCreateEntry(key);

                entry.recordSlowCall(duration);
                newCallCount = overallSlowCalls.incrementAndGet();
                if (newCallCount < 0) { //can just happen in case of an overflow
                    resetOverallCallStats();
                }
            } else {
                newCallCount = overallFastCalls.incrementAndGet();
                if (newCallCount < 0) { //can just happen in case of an overflow
                    resetOverallCallStats();
                }
            }
        } catch (Throwable t) {
            //don't handle exceptions during metrics-handling
        }
    }

    /**
     * Calculates the percentage of slow calls relative to all recorded calls.
     *
     * @return the percentage of slow calls
     */
    public double calcPercentageOfSlowCalls() {
        long slowCalls = overallSlowCalls.get();
        long fastCalls = overallFastCalls.get();

        if (slowCalls == 0L) {
            return 0L;
        }
        return new BigDecimal(100).divide(new BigDecimal(slowCalls + fastCalls), 10, RoundingMode.HALF_UP).multiply(new BigDecimal(slowCalls)).longValue();
    }

    /**
     * Calculates the overall average duration per method key.
     *
     * @return a map of method key to average duration in milliseconds
     */
    public Map<String, Long> calcOverallAverage() {
        return calcOverallAverage(-1L);
    }

    /**
     * Calculates the overall average duration per method key up to the given time slot.
     *
     * @param valueOfTheLatestTimeSlot the upper bound for time slots, or negative for current time
     * @return a map of method key to average duration in milliseconds
     */
    public Map<String, Long> calcOverallAverage(long valueOfTheLatestTimeSlot) {
        if (valueOfTheLatestTimeSlot <= 0) {
            valueOfTheLatestTimeSlot = MetricsEntry.createCurrentKey();
        }

        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, MetricsEntry> entry : entries.entrySet()) {
            MetricsEntry metricsEntry = entry.getValue();

            if (metricsEntry == null) {
                continue;
            }

            //since we just have 1 entry per second we can't get an endless loop (just because new calls get recorded in parallel)
            //esp. because we just record slow calls and calls which finish in the same second get recorded by the same entry
            double duration = 0L;
            long numberOfEntries = 0L;
            for (StatsEntry statsEntry : metricsEntry.getStatsEntries(valueOfTheLatestTimeSlot)) {
                if (statsEntry.getNumberOfCalls() == 0) { //the entry was just created (as an empty entry and a parallel thread is going to update it soon)
                    continue;
                }

                duration += statsEntry.getAverageDuration();
                numberOfEntries++; //don't use the number of calls (it's used for the average-calc)
            }

            if (numberOfEntries > 0L) {
                result.put(entry.getKey(), new BigDecimal(duration).divide(new BigDecimal(numberOfEntries), 10, RoundingMode.HALF_UP).longValue());
            }
        }

        return result;
    }

    /**
     * Calculates the overall minimum duration per method key.
     *
     * @return a map of method key to minimum duration in milliseconds
     */
    public Map<String, Long> calcOverallMin() {
        return calcOverallMin(-1L);
    }

    /**
     * Calculates the overall minimum duration per method key up to the given time slot.
     *
     * @param valueOfTheLatestTimeSlot the upper bound for time slots, or negative for current time
     * @return a map of method key to minimum duration in milliseconds
     */
    public Map<String, Long> calcOverallMin(long valueOfTheLatestTimeSlot) {
        if (valueOfTheLatestTimeSlot <= 0) {
            valueOfTheLatestTimeSlot = MetricsEntry.createCurrentKey();
        }

        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, MetricsEntry> entry : entries.entrySet()) {
            MetricsEntry metricsEntry = entry.getValue();

            if (metricsEntry == null) {
                continue;
            }

            //since we just have 1 entry per second we can't get an endless loop (just because new calls get recorded in parallel)
            //esp. because we just record slow calls and calls which finish in the same second get recorded by the same entry
            long currentMin;
            long globalMin = Long.MAX_VALUE;
            for (StatsEntry statsEntry : metricsEntry.getStatsEntries(valueOfTheLatestTimeSlot)) {
                if (statsEntry.getNumberOfCalls() == 0) { //the entry was just created (as an empty entry and a parallel thread is going to update it soon)
                    continue;
                }

                currentMin = statsEntry.getMinDuration();
                if (currentMin < globalMin) {
                    globalMin = currentMin;
                }
            }

            result.put(entry.getKey(), globalMin);
        }
        return result;
    }

    /**
     * Calculates the overall maximum duration per method key.
     *
     * @return a map of method key to maximum duration in milliseconds
     */
    public Map<String, Long> calcOverallMax() {
        return calcOverallMax(-1L);
    }

    /**
     * Calculates the overall maximum duration per method key up to the given time slot.
     *
     * @param valueOfTheLatestTimeSlot the upper bound for time slots, or negative for current time
     * @return a map of method key to maximum duration in milliseconds
     */
    public Map<String, Long> calcOverallMax(long valueOfTheLatestTimeSlot) {
        if (valueOfTheLatestTimeSlot <= 0) {
            valueOfTheLatestTimeSlot = MetricsEntry.createCurrentKey();
        }

        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, MetricsEntry> entry : entries.entrySet()) {
            MetricsEntry metricsEntry = entry.getValue();

            if (metricsEntry == null) {
                continue;
            }

            //since we just have 1 entry per second we can't get an endless loop (just because new calls get recorded in parallel)
            //esp. because we just record slow calls and calls which finish in the same second get recorded by the same entry
            long currentMax;
            long globalMax = 0L;
            for (StatsEntry statsEntry : metricsEntry.getStatsEntries(valueOfTheLatestTimeSlot)) {
                if (statsEntry.getNumberOfCalls() == 0) { //the entry was just created (as an empty entry and a parallel thread is going to update it soon)
                    continue;
                }

                currentMax = statsEntry.getMaxDuration();
                if (currentMax > globalMax) {
                    globalMax = currentMax;
                }
            }

            result.put(entry.getKey(), globalMax);
        }
        return result;
    }

    /**
     * Calculates the given percentile of average durations per method key.
     *
     * @param percentage the percentile as a decimal (e.g. 0.95 for 95th percentile)
     * @return a map of method key to percentile duration in milliseconds
     */
    public Map<String, Long> calcPercentile(double percentage) {
        return calcPercentile(percentage, -1L);
    }

    /**
     * Calculates the given percentile of average durations per method key up to the given time slot.
     *
     * @param percentage               the percentile as a decimal (e.g. 0.95 for 95th percentile)
     * @param valueOfTheLatestTimeSlot the upper bound for time slots, or negative for current time
     * @return a map of method key to percentile duration in milliseconds
     */
    public Map<String, Long> calcPercentile(double percentage, long valueOfTheLatestTimeSlot) {
        if (valueOfTheLatestTimeSlot <= 0) {
            valueOfTheLatestTimeSlot = MetricsEntry.createCurrentKey();
        }

        if (percentage > 0.99) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, MetricsEntry> entry : entries.entrySet()) {
            MetricsEntry metricsEntry = entry.getValue();

            if (metricsEntry == null) {
                continue;
            }

            //since we just have 1 entry per second we can't get an endless loop (just because new calls get recorded in parallel)
            //esp. because we just record slow calls and calls which finish in the same second get recorded by the same entry
            List<Long> averageListAcrossTimeslots = new ArrayList<>();
            for (StatsEntry statsEntry : metricsEntry.getStatsEntries(valueOfTheLatestTimeSlot)) {
                if (statsEntry.getNumberOfCalls() == 0) { //the entry was just created (as an empty entry and a parallel thread is going to update it soon)
                    continue;
                }

                averageListAcrossTimeslots.add(Double.valueOf(statsEntry.getAverageDuration()).longValue());
            }

            if (averageListAcrossTimeslots.isEmpty()) {
                continue;
            }

            Collections.sort(averageListAcrossTimeslots);

            int index = new BigDecimal(averageListAcrossTimeslots.size()).multiply(new BigDecimal(percentage)).intValue();
            if (index > 0) {
                averageListAcrossTimeslots = averageListAcrossTimeslots.subList(0, index);
            }
            double duration = 0L;
            for (Long currentDuration : averageListAcrossTimeslots) {
                duration += currentDuration;
            }

            BigDecimal divisor = new BigDecimal(averageListAcrossTimeslots.size());
            if (divisor.intValue() == 0) {
                result.put(entry.getKey(), Double.valueOf(duration).longValue());
            } else {
                result.put(entry.getKey(), new BigDecimal(duration).divide(divisor, 10, RoundingMode.HALF_UP).longValue());
            }
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

    /**
     * Observes circuit-open events and records them in the corresponding metrics entry.
     *
     * @param circuitEvent the circuit event
     */
    public void onOpenCircuit(@Observes @CircuitState(CircuitState.Value.OPEN) CircuitEvent circuitEvent) {
        MetricsEntry entry = getOrCreateEntry(circuitEvent.getMethodKey());
        entry.onOpenCircuit();
    }

    /**
     * Observes circuit-half-open events and records them in the corresponding metrics entry.
     *
     * @param circuitEvent the circuit event
     */
    public void onHalfOpenCircuit(@Observes @CircuitState(CircuitState.Value.HALF_OPEN) CircuitEvent circuitEvent) {
        MetricsEntry entry = getOrCreateEntry(circuitEvent.getMethodKey());
        entry.onHalfOpenCircuit();
    }

    /**
     * Observes circuit-close events and records them in the corresponding metrics entry.
     *
     * @param circuitEvent the circuit event
     */
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

    private void resetOverallCallStats() {
        overallSlowCalls = new AtomicLong(0);
        overallFastCalls = new AtomicLong(0);
    }
}
