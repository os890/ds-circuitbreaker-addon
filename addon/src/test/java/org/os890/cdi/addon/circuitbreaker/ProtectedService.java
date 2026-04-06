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

package org.os890.cdi.addon.circuitbreaker;

import jakarta.enterprise.context.ApplicationScoped;

import org.os890.cdi.addon.circuitbreaker.api.FailureThreshold;
import org.os890.cdi.addon.circuitbreaker.api.OverloadProtection;
import org.os890.cdi.addon.circuitbreaker.api.SuccessThreshold;

/**
 * Test service with circuit-breaker protection for use in integration tests.
 */
@ApplicationScoped
public class ProtectedService {

    private boolean shouldFail;

    /**
     * A protected method that can be toggled to fail.
     *
     * @return a greeting string
     * @throws RuntimeException if {@link #setShouldFail(boolean)} was set to {@code true}
     */
    @OverloadProtection(collectMetrics = false)
    @FailureThreshold(failures = 2, executions = 3)
    @SuccessThreshold(2)
    public String doWork() {
        if (shouldFail) {
            throw new RuntimeException("simulated failure");
        }
        return "ok";
    }

    /**
     * Toggles whether {@link #doWork()} should throw an exception.
     *
     * @param shouldFail {@code true} to make calls fail
     */
    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }
}
