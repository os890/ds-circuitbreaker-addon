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

import java.lang.reflect.Method;

/**
 * Descriptor that uniquely identifies a circuit breaker by a key derived
 * from the protected method's class, name, and parameter types.
 */
public class CircuitBreakerDescriptor {

    private final String key;
    private final Method currentMethod;

    /**
     * Creates a new descriptor.
     *
     * @param key           the unique key for this circuit breaker
     * @param currentMethod the method being protected
     */
    public CircuitBreakerDescriptor(String key, Method currentMethod) {
        this.key = key;
        this.currentMethod = currentMethod;
    }

    /**
     * Returns the unique key for this circuit breaker.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the method being protected.
     *
     * @return the current method
     */
    public Method getCurrentMethod() {
        return currentMethod;
    }
}
