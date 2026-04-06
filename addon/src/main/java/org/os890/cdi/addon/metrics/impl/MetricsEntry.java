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

import org.apache.deltaspike.core.api.config.ConfigResolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores time-bucketed {@link StatsEntry} records for a single protected method,
 * with one entry per second of wall-clock time.
 */
public class MetricsEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Long, StatsEntry> statsEntries;
    private static Integer maxStatsEntries;

    /**
     * Creates a new metrics entry, initialising the maximum entry count from
     * DeltaSpike configuration if not already set.
     */
    public MetricsEntry() {
        if (maxStatsEntries == null) {
            //max 12 hours of data if there is a request for every second
            String configuredValue = ConfigResolver.getProjectStageAwarePropertyValue(
                    MetricsEntry.class.getSimpleName() + "_maxCount",
                    "" + (12 /*hours*/ * 60 /*min*/ * 60 /*sec*/));
            maxStatsEntries = Integer.parseInt(configuredValue);
        }
        statsEntries = new ConcurrentHashMap<>(maxStatsEntries + 1);
    }

    /**
     * Records a slow call with the given duration.
     *
     * @param duration the call duration in milliseconds
     */
    public void recordSlowCall(long duration) {
        StatsEntry currentEntry = getOrCreateCurrentEntry();
        currentEntry.recordCall(duration);
    }

    /**
     * Returns the raw map of time-keyed stats entries.
     *
     * @return the stats entries per second
     */
    public Map<Long, StatsEntry> getStatsEntriesPerSecond() {
        return statsEntries;
    }

    /**
     * Returns all stats entries as a collection.
     *
     * @return a snapshot of all stats entries
     */
    public Collection<StatsEntry> getStatsEntries() {
        return new ArrayList<>(statsEntries.values());
    }

    /**
     * Returns stats entries up to the given time-slot boundary.
     *
     * @param timeSlotBoarder the upper bound for time-slot keys (inclusive)
     * @return stats entries within the boundary
     */
    public Collection<StatsEntry> getStatsEntries(long timeSlotBoarder) {
        List<StatsEntry> result = new ArrayList<>();

        for (Map.Entry<Long, StatsEntry> entry : statsEntries.entrySet()) {
            Long timeSlotKey = entry.getKey();

            if (timeSlotKey == null) {
                continue;
            }
            if (timeSlotKey <= timeSlotBoarder) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    /**
     * Records an open-circuit event in the current time slot.
     */
    public synchronized void onOpenCircuit() {
        StatsEntry currentEntry = getOrCreateCurrentEntry();
        currentEntry.onOpenCircuit();
    }

    /**
     * Records a half-open-circuit event in the current time slot.
     */
    public synchronized void onHalfOpenCircuit() {
        StatsEntry currentEntry = getOrCreateCurrentEntry();
        currentEntry.onHalfOpenCircuit();
    }

    /**
     * Records a close-circuit event in the current time slot.
     */
    public synchronized void onCloseCircuit() {
        StatsEntry currentEntry = getOrCreateCurrentEntry();
        currentEntry.onCloseCircuit();
    }

    private StatsEntry getOrCreateCurrentEntry() {
        long timeInSeconds = createCurrentKey();
        StatsEntry statsEntry = statsEntries.get(timeInSeconds);

        if (statsEntry == null) {
            statsEntry = createNewEntry(timeInSeconds);
        }
        return statsEntry;
    }

    private synchronized StatsEntry createNewEntry(long timeInSeconds) {
        StatsEntry statsEntry = statsEntries.get(timeInSeconds);

        if (statsEntry != null) {
            return statsEntry;
        }
        statsEntry = new StatsEntry();
        statsEntries.put(timeInSeconds, statsEntry);

        if (statsEntries.size() > maxStatsEntries) {
            new AsyncMetricsCollector(statsEntries, maxStatsEntries).start();
        }
        return statsEntry;
    }

    /**
     * Creates a time key based on the current system time in seconds.
     *
     * @return the current time in seconds since epoch
     */
    public static long createCurrentKey() {
        return System.currentTimeMillis() / 1000;
    }
}
