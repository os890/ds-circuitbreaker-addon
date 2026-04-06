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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asynchronous cleanup task that removes old {@link StatsEntry} records
 * from the metrics storage to prevent unbounded memory growth.
 */
public class AsyncMetricsCollector {

    private static final AtomicInteger ONGOING_CLEANUP_TASK_COUNT = new AtomicInteger(0);

    private final Map<Long, StatsEntry> statsEntriesToCleanup;
    private int maxStatsEntries;
    private final long timeBoarder;

    /**
     * Creates a new collector for the given entries map.
     *
     * @param statsEntriesToCleanup the map of time-keyed stats entries to clean up
     * @param maxStatsEntries       the maximum number of entries to retain
     */
    public AsyncMetricsCollector(Map<Long, StatsEntry> statsEntriesToCleanup, int maxStatsEntries) {
        this.statsEntriesToCleanup = statsEntriesToCleanup;
        this.maxStatsEntries = maxStatsEntries;
        long valueOfTheLatestKey = MetricsEntry.createCurrentKey();
        timeBoarder = valueOfTheLatestKey - (maxStatsEntries * 2 / 3 /*just keep 2/3 of the data that we don't get cleanups over and over again*/);
    }

    /**
     * Starts the asynchronous cleanup. At most three concurrent cleanup tasks
     * are allowed; additional calls are silently skipped.
     */
    public void start() {
        int ongoingTaskCount = ONGOING_CLEANUP_TASK_COUNT.incrementAndGet();

        if (ongoingTaskCount > 3) {
            return;
        }

        UnmanagedExecutorHelper.execute(() -> {
            try {
                Set<Long> keys = statsEntriesToCleanup.keySet();
                for (Long key : keys) {
                    if (key < timeBoarder) {
                        statsEntriesToCleanup.remove(key);
                    }
                }
            } finally {
                int numberOfScheduledTasks = ONGOING_CLEANUP_TASK_COUNT.decrementAndGet();

                if (numberOfScheduledTasks == 0 && statsEntriesToCleanup.size() > maxStatsEntries) {
                    statsEntriesToCleanup.clear(); //emergency cleanup - there is a leak - TODO log entry
                }
            }
        });
    }
}
