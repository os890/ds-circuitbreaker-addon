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

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.os890.cdi.addon.circuitbreaker.api.ServiceOverloadedException;
import org.os890.cdi.addon.dynamictestbean.EnableTestBeans;

/**
 * Integration test that verifies the full circuit-breaker lifecycle:
 * closed → open → half-open → closed.
 *
 * <p>Uses {@link RecoverableService} which has a 200ms open delay so the
 * test can wait for the circuit to transition to half-open without a long pause.</p>
 */
@EnableTestBeans
class CircuitBreakerRecoveryTest {

    @Inject
    private RecoverableService recoverableService;

    /**
     * Verifies the full recovery cycle:
     * <ol>
     *   <li>Cause failures until the circuit opens</li>
     *   <li>Confirm calls are rejected with {@link ServiceOverloadedException}</li>
     *   <li>Wait for the open delay to expire (circuit transitions to half-open)</li>
     *   <li>Fix the service so calls succeed again</li>
     *   <li>Confirm the circuit closes and calls pass through normally</li>
     * </ol>
     *
     * @throws InterruptedException if the sleep is interrupted
     */
    @Test
    void circuitRecoversAfterOpenDelay() throws InterruptedException {
        // 1. Trip the circuit by causing failures
        recoverableService.setShouldFail(true);

        boolean circuitOpened = false;
        for (int i = 0; i < 10; i++) {
            try {
                recoverableService.call();
            } catch (ServiceOverloadedException e) {
                circuitOpened = true;
                break;
            } catch (RuntimeException e) {
                // counting toward threshold
            }
        }
        Assertions.assertTrue(circuitOpened, "Circuit should have opened");

        // 2. Confirm the circuit is open — calls are rejected
        Assertions.assertThrows(ServiceOverloadedException.class,
                () -> recoverableService.call());

        // 3. Fix the service and wait for the open delay (200ms) to expire
        recoverableService.setShouldFail(false);
        Thread.sleep(400);

        // 4. The circuit should now be half-open, allowing a trial call.
        //    A successful trial closes the circuit (SuccessThreshold = 1).
        String result = recoverableService.call();
        Assertions.assertEquals("recovered", result);

        // 5. Confirm the circuit is fully closed — subsequent calls work too
        Assertions.assertEquals("recovered", recoverableService.call());
        Assertions.assertEquals("recovered", recoverableService.call());
    }
}
