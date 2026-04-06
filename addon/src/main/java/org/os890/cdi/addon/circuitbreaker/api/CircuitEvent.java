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

package org.os890.cdi.addon.circuitbreaker.api;

/**
 * Event fired when a circuit breaker changes state.
 *
 * <p>Instances carry the method key identifying the protected method and
 * the new {@link CircuitState.Value} of the circuit.</p>
 */
public class CircuitEvent {

    private final String methodKey;
    private final CircuitState.Value circuitStateValue;

    /**
     * Creates a new circuit event.
     *
     * @param methodKey         the key identifying the protected method
     * @param circuitStateValue the new state of the circuit breaker
     */
    public CircuitEvent(String methodKey, CircuitState.Value circuitStateValue) {
        this.methodKey = methodKey;
        this.circuitStateValue = circuitStateValue;
    }

    /**
     * Returns the key identifying the protected method.
     *
     * @return the method key
     */
    public String getMethodKey() {
        return methodKey;
    }

    /**
     * Returns the new state of the circuit breaker.
     *
     * @return the circuit state value
     */
    public CircuitState.Value getCircuitStateValue() {
        return circuitStateValue;
    }
}
