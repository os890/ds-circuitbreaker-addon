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

package org.os890.cdi.addon.circuitbreaker.impl;

import dev.failsafe.CircuitBreaker;
import org.os890.cdi.addon.circuitbreaker.api.CircuitEvent;
import org.os890.cdi.addon.circuitbreaker.api.CircuitOpenDelay;
import org.os890.cdi.addon.circuitbreaker.api.CircuitState;
import org.os890.cdi.addon.circuitbreaker.api.FailureThreshold;
import org.os890.cdi.addon.circuitbreaker.api.SuccessThreshold;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Application-scoped provider that creates and caches {@link CircuitBreaker}
 * instances per protected method.
 *
 * <p>Each circuit breaker is configured from annotations on the target method
 * and fires CDI events on state transitions.</p>
 */
@ApplicationScoped
public class CircuitBreakerProvider {

    //individual broadcasters instead of #select to get a better perf.

    @Inject
    @CircuitState(CircuitState.Value.OPEN)
    private Event<CircuitEvent> circuitOpenBroadcaster;

    @Inject
    @CircuitState(CircuitState.Value.HALF_OPEN)
    private Event<CircuitEvent> circuitHalfOpenBroadcaster;

    @Inject
    @CircuitState(CircuitState.Value.CLOSED)
    private Event<CircuitEvent> circuitClosedBroadcaster;

    private Map<String, CircuitBreaker<Object>> circuitBreakerMap = new HashMap<>();

    /**
     * Returns the circuit breaker for the given descriptor, creating it if necessary.
     *
     * @param circuitBreakerDescriptor the descriptor identifying the circuit breaker
     * @return the circuit breaker instance
     */
    public CircuitBreaker<Object> getCircuitBreakerFor(CircuitBreakerDescriptor circuitBreakerDescriptor) {
        CircuitBreaker<Object> circuitBreaker = circuitBreakerMap.get(circuitBreakerDescriptor.getKey());

        if (circuitBreaker == null) {
            circuitBreaker = buildCircuitBreaker(circuitBreakerDescriptor);
        }
        return circuitBreaker;
    }

    private synchronized CircuitBreaker<Object> buildCircuitBreaker(CircuitBreakerDescriptor circuitBreakerDescriptor) {
        CircuitBreaker<Object> circuitBreaker = circuitBreakerMap.get(circuitBreakerDescriptor.getKey());

        if (circuitBreaker != null) {
            return circuitBreaker;
        }

        Method currentMethod = circuitBreakerDescriptor.getCurrentMethod();

        FailureThreshold failureThreshold = currentMethod.getAnnotation(FailureThreshold.class);
        if (failureThreshold == null) {
            failureThreshold = FailureThreshold.DEFAULT;
        }

        SuccessThreshold successThreshold = currentMethod.getAnnotation(SuccessThreshold.class);
        if (successThreshold == null) {
            successThreshold = SuccessThreshold.DEFAULT;
        }

        CircuitOpenDelay circuitOpenDelay = currentMethod.getAnnotation(CircuitOpenDelay.class);
        if (circuitOpenDelay == null) {
            circuitOpenDelay = CircuitOpenDelay.DEFAULT;
        }

        String key = circuitBreakerDescriptor.getKey();

        circuitBreaker = CircuitBreaker.<Object>builder()
                .withFailureThreshold(failureThreshold.failures(), failureThreshold.executions())
                .withSuccessThreshold(successThreshold.value())
                .withDelay(Duration.of(circuitOpenDelay.delay(), circuitOpenDelay.timeUnit().toChronoUnit()))
                .onOpen(e -> broadcastOpenCircuit(key))
                .onHalfOpen(e -> broadcastHalfOpenCircuit(key))
                .onClose(e -> broadcastCloseCircuit(key))
                .build();

        circuitBreakerMap.put(key, circuitBreaker);

        return circuitBreaker;
    }

    private void broadcastOpenCircuit(String key) {
        circuitOpenBroadcaster.fire(new CircuitEvent(key, CircuitState.Value.OPEN));
    }

    private void broadcastHalfOpenCircuit(String key) {
        circuitHalfOpenBroadcaster.fire(new CircuitEvent(key, CircuitState.Value.HALF_OPEN));
    }

    private void broadcastCloseCircuit(String key) {
        circuitClosedBroadcaster.fire(new CircuitEvent(key, CircuitState.Value.CLOSED));
    }
}
