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
import net.jodah.failsafe.CircuitBreakerOpenException;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.os890.cdi.addon.circuitbreaker.api.*;

import javax.annotation.Priority;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

@Priority(1)
@Interceptor
@OverloadProtection
public class OverloadProtectionInterceptor implements Serializable {
    private static final long serialVersionUID = 14L;

    private static Integer filterMethodsFasterThanMs; //additional perf. improvement to avoid metrics-overhead for very fast methods (leads to a ~30% better performance if all methods are faster)

    public OverloadProtectionInterceptor() {
        if (filterMethodsFasterThanMs == null) {
            String configuredValue = ConfigResolver.getProjectStageAwarePropertyValue(OverloadProtection.class.getSimpleName() + "_filterMethodsFasterThanMs", "1");
            filterMethodsFasterThanMs = Integer.parseInt(configuredValue);
        }
    }

    @Inject
    private CircuitBreakerProvider circuitBreakerProvider;

    @Inject
    private Event<ProtectedCallEvent> protectedCallBroadcaster;

    @AroundInvoke
    public Object execute(final InvocationContext invocationContext) throws Exception {
        try {
            final Method currentMethod = invocationContext.getMethod();
            final CircuitBreakerDescriptor circuitBreakerDescriptor = createDescriptor(currentMethod);
            CircuitBreaker circuitBreaker = circuitBreakerProvider.getCircuitBreakerFor(circuitBreakerDescriptor);

            return Failsafe.with(circuitBreaker).get(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    long start = System.currentTimeMillis();
                    try {
                        return invocationContext.proceed();
                    } finally {
                        long duration = System.currentTimeMillis() - start;

                        if (duration > filterMethodsFasterThanMs) { //don't eval the optional annotation (@FilterMethodsFasterThan) here - to avoid an impact on perf. (~20%)
                            OverloadProtection overloadProtection = currentMethod.getAnnotation(OverloadProtection.class);

                            if (overloadProtection != null && overloadProtection.collectMetrics()) {
                                protectedCallBroadcaster.fire(new ProtectedCallEvent(circuitBreakerDescriptor.getKey(), currentMethod, duration));
                            }
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
            for (Class paramType : currentMethod.getParameterTypes()) {
                keyBuilder.append("|").append(paramType.getName());
            }
        }

        final String key = keyBuilder.toString();

        CircuitBreakerDescriptor circuitBreakerDescriptor = new CircuitBreakerDescriptor(key, currentMethod);
        return circuitBreakerDescriptor;
    }
}
