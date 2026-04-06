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

import org.os890.cdi.addon.circuitbreaker.api.CircuitState;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds aggregated statistics for a single time slot (one second):
 * call count, min/max/total duration, and circuit-breaker state.
 */
public class StatsEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private CircuitState.Value circuitState = CircuitState.Value.CLOSED;

    private long minDuration = 0L;
    private long maxDuration = 0L;
    private long duration = 0L;
    private AtomicInteger numberOfCalls = new AtomicInteger(0);

    /**
     * Records a call with the given duration, updating min, max, and total.
     *
     * @param newDuration the call duration in milliseconds
     */
    public void recordCall(long newDuration) {
        if (duration == 0L) {
            duration = newDuration;
            minDuration = newDuration;
            maxDuration = newDuration;
        } else {
            duration += newDuration;
            if (newDuration > maxDuration) {
                maxDuration = newDuration;
            } else if (newDuration < minDuration) {
                minDuration = newDuration;
            }
        }
        numberOfCalls.incrementAndGet();
    }

    void onOpenCircuit() {
        circuitState = CircuitState.Value.OPEN;
    }

    void onHalfOpenCircuit() {
        circuitState = CircuitState.Value.HALF_OPEN;
    }

    void onCloseCircuit() {
        circuitState = CircuitState.Value.CLOSED;
    }

    /**
     * Calculates the average call duration for this time slot.
     *
     * @return the average duration, or {@code 0} if no calls recorded
     */
    public double getAverageDuration() {
        if (numberOfCalls.get() == 0L) {
            return 0L;
        }
        return new BigDecimal(duration).divide(new BigDecimal(numberOfCalls.get()), 10, RoundingMode.HALF_UP).doubleValue();
    }

    /*
     * generated
     */

    /**
     * Returns the minimum call duration recorded in this time slot.
     *
     * @return the minimum duration in milliseconds
     */
    public long getMinDuration() {
        return minDuration;
    }

    /**
     * Returns the maximum call duration recorded in this time slot.
     *
     * @return the maximum duration in milliseconds
     */
    public long getMaxDuration() {
        return maxDuration;
    }

    /**
     * Returns the number of calls recorded in this time slot.
     *
     * @return the call count
     */
    public int getNumberOfCalls() {
        return numberOfCalls.get();
    }

    /**
     * Returns the circuit-breaker state at this time slot.
     *
     * @return the circuit state
     */
    public CircuitState.Value getCircuitState() {
        return circuitState;
    }
}
