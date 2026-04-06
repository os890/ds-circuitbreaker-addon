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
import dev.failsafe.CircuitBreakerOpenException;
import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeException;
import dev.failsafe.Timeout;
import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.os890.cdi.addon.circuitbreaker.api.ExecutionFailure;
import org.os890.cdi.addon.circuitbreaker.api.OverloadProtection;
import org.os890.cdi.addon.circuitbreaker.api.ProtectedCallEvent;
import org.os890.cdi.addon.circuitbreaker.api.ServiceOverloadedException;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.Duration;

/**
 * CDI interceptor that wraps method invocations with a Failsafe
 * {@link CircuitBreaker} to protect against service overload.
 *
 * <p>When the circuit is open, a {@link ServiceOverloadedException} is thrown.
 * Metrics are optionally collected and broadcast as CDI events.</p>
 */
@Priority(1)
@Interceptor
@OverloadProtection
public class OverloadProtectionInterceptor implements Serializable {

    private static final long serialVersionUID = 14L;

    private static Integer filterMethodsFasterThanMs; //additional perf. improvement to avoid metrics-overhead for very fast methods (leads to a ~30% better performance if all methods are faster)

    /**
     * Default constructor that initialises the method-filter threshold from DeltaSpike configuration.
     */
    public OverloadProtectionInterceptor() {
        if (filterMethodsFasterThanMs == null) {
            String configuredValue = ConfigResolver.getProjectStageAwarePropertyValue(
                    OverloadProtection.class.getSimpleName() + "_filterMethodsFasterThanMs", "1");
            filterMethodsFasterThanMs = Integer.parseInt(configuredValue);
        }
    }

    @Inject
    private CircuitBreakerProvider circuitBreakerProvider;

    @Inject
    private Event<ProtectedCallEvent> protectedCallBroadcaster;

    /**
     * Intercepts the method invocation and applies circuit-breaker protection.
     *
     * @param invocationContext the interceptor invocation context
     * @return the result of the intercepted method
     * @throws Exception if the method throws or the circuit is open
     */
    @AroundInvoke
    public Object execute(InvocationContext invocationContext) throws Exception {
        try {
            Method currentMethod = invocationContext.getMethod();
            CircuitBreakerDescriptor circuitBreakerDescriptor = createDescriptor(currentMethod);
            CircuitBreaker<Object> circuitBreaker = circuitBreakerProvider.getCircuitBreakerFor(circuitBreakerDescriptor);

            ExecutionFailure executionFailure = currentMethod.getAnnotation(ExecutionFailure.class);
            if (executionFailure == null) {
                executionFailure = ExecutionFailure.DEFAULT;
            }

            Timeout<Object> timeout = Timeout.<Object>builder(
                    Duration.of(executionFailure.after(), executionFailure.timeUnit().toChronoUnit()))
                    .build();

            return Failsafe.with(circuitBreaker, timeout).get(() -> {
                long start = System.currentTimeMillis();
                try {
                    return invocationContext.proceed();
                } finally {
                    long duration = System.currentTimeMillis() - start;

                    if (duration > filterMethodsFasterThanMs) { //don't eval the optional annotation (@FilterMethodsFasterThan) here - to avoid an impact on perf. (~20%)
                        OverloadProtection overloadProtection = currentMethod.getAnnotation(OverloadProtection.class);

                        if (overloadProtection != null && overloadProtection.collectMetrics()) {
                            protectedCallBroadcaster.fire(
                                    new ProtectedCallEvent(circuitBreakerDescriptor.getKey(), currentMethod, duration));
                        }
                    }
                }
            });
        } catch (CircuitBreakerOpenException e) {
            throw new ServiceOverloadedException(e);
        } catch (FailsafeException e) {
            Throwable cause = e.getCause();

            if (cause != null) {
                throw ExceptionUtils.throwAsRuntimeException(cause);
            }
            throw e;
        }
    }

    private CircuitBreakerDescriptor createDescriptor(Method currentMethod) {
        StringBuilder keyBuilder = new StringBuilder(currentMethod.getDeclaringClass() + "#" + currentMethod.getName());

        if (currentMethod.getParameterTypes().length > 0) {
            for (Class<?> paramType : currentMethod.getParameterTypes()) {
                keyBuilder.append("|").append(paramType.getName());
            }
        }

        String key = keyBuilder.toString();

        CircuitBreakerDescriptor circuitBreakerDescriptor = new CircuitBreakerDescriptor(key, currentMethod);
        return circuitBreakerDescriptor;
    }
}
