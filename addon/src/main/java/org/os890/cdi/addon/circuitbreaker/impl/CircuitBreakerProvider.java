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

import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.function.CheckedRunnable;
import org.os890.cdi.addon.circuitbreaker.api.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    private Map<String, CircuitBreaker> circuitBreakerMap = new HashMap<String, CircuitBreaker>();

    public CircuitBreaker getCircuitBreakerFor(final CircuitBreakerDescriptor circuitBreakerDescriptor) {
        CircuitBreaker circuitBreaker = circuitBreakerMap.get(circuitBreakerDescriptor.getKey());

        if (circuitBreaker == null) {
            circuitBreaker = buildCircuitBreaker(circuitBreakerDescriptor);
        }
        return circuitBreaker;
    }

    private synchronized CircuitBreaker buildCircuitBreaker(final CircuitBreakerDescriptor circuitBreakerDescriptor) {
        CircuitBreaker circuitBreaker = circuitBreakerMap.get(circuitBreakerDescriptor.getKey());

        if (circuitBreaker != null) {
            return circuitBreaker;
        }

        Method currentMethod = circuitBreakerDescriptor.getCurrentMethod();

        FailureThreshold failureThreshold = currentMethod.getAnnotation(FailureThreshold.class);
        if (failureThreshold == null) {
            failureThreshold = new FailureThreshold.Literal();
        }

        SuccessThreshold successThreshold = currentMethod.getAnnotation(SuccessThreshold.class);
        if (successThreshold == null) {
            successThreshold = new SuccessThreshold.Literal();
        }

        CircuitOpenDelay circuitOpenDelay = currentMethod.getAnnotation(CircuitOpenDelay.class);
        if (circuitOpenDelay == null) {
            circuitOpenDelay = new CircuitOpenDelay.Literal();
        }

        ExecutionFailure executionFailure = currentMethod.getAnnotation(ExecutionFailure.class);
        if (executionFailure == null) {
            executionFailure = new ExecutionFailure.Literal();
        }

        //TODO read values from config
        circuitBreaker = new CircuitBreaker()
                .withFailureThreshold(failureThreshold.failures(), failureThreshold.executions())
                .withSuccessThreshold(successThreshold.value())
                .withDelay(circuitOpenDelay.delay(), circuitOpenDelay.timeUnit())
                .withTimeout(executionFailure.after(), executionFailure.timeUnit());

        circuitBreaker.onOpen(new CheckedRunnable() {
            @Override
            public void run() throws Exception {
                broadcastOpenCircuit(circuitBreakerDescriptor.getKey());
            }
        });
        circuitBreaker.onHalfOpen(new CheckedRunnable() {
            @Override
            public void run() throws Exception {
                broadcastHalfOpenCircuit(circuitBreakerDescriptor.getKey());
            }
        });
        circuitBreaker.onClose(new CheckedRunnable() {
            @Override
            public void run() throws Exception {
                broadcastCloseCircuit(circuitBreakerDescriptor.getKey());
            }
        });

        circuitBreakerMap.put(circuitBreakerDescriptor.getKey(), circuitBreaker);

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
