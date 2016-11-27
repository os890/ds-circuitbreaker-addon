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
import java.util.concurrent.atomic.AtomicInteger;

public class StatsEntry implements Serializable {
    private CircuitState.Value circuitState = CircuitState.Value.CLOSED;

    private long minDuration = 0L;
    private long maxDuration = 0L;
    private long duration = 0L;
    private AtomicInteger numberOfCalls = new AtomicInteger(0);

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

    public double getAverageDuration() {
        if (numberOfCalls.get() == 0L) {
            return 0L;
        }
        return new BigDecimal(duration).divide(new BigDecimal(numberOfCalls.get()), 10, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /*
     * generated
     */

    public long getMinDuration() {
        return minDuration;
    }

    public long getMaxDuration() {
        return maxDuration;
    }

    public int getNumberOfCalls() {
        return numberOfCalls.get();
    }

    public CircuitState.Value getCircuitState() {
        return circuitState;
    }
}
