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

import java.lang.reflect.Method;

/**
 * Event fired after a protected method call completes, carrying the call
 * duration for metrics collection.
 */
public class ProtectedCallEvent {

    private final String key;
    private Method currentMethod;
    private final long duration;

    /**
     * Creates a new protected call event.
     *
     * @param key           the key identifying the protected method
     * @param currentMethod the method that was invoked
     * @param duration      the call duration in milliseconds
     */
    public ProtectedCallEvent(String key, Method currentMethod, long duration) {
        this.key = key;
        this.currentMethod = currentMethod;
        this.duration = duration;
    }

    /**
     * Returns the key identifying the protected method.
     *
     * @return the method key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the method that was invoked.
     *
     * @return the current method
     */
    public Method getCurrentMethod() {
        return currentMethod;
    }

    /**
     * Returns the call duration in milliseconds.
     *
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }
}
