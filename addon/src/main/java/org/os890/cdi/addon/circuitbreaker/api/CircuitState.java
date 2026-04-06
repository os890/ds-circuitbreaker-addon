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

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * CDI qualifier for observing circuit-breaker state-change events.
 *
 * <p>The {@link Value} enum can be used to observe specific circuit-breaker
 * transitions such as {@code OPEN}, {@code HALF_OPEN}, or {@code CLOSED}.</p>
 */
@Qualifier
@Target({TYPE, METHOD, PARAMETER, FIELD})
@Retention(RUNTIME)
@Documented
public @interface CircuitState {

    /**
     * The circuit state value to qualify.
     *
     * @return the circuit state
     */
    CircuitState.Value value();

    /**
     * Enumeration of possible circuit-breaker states.
     */
    enum Value {
        /** The circuit is open and rejecting calls. */
        OPEN,
        /** The circuit is half-open and allowing trial calls. */
        HALF_OPEN,
        /** The circuit is closed and operating normally. */
        CLOSED
    }

    /**
     * Annotation literal for programmatic use of {@link CircuitState}.
     */
    class Literal extends AnnotationLiteral<CircuitState> implements CircuitState {

        private static final long serialVersionUID = 7310730593030223981L;

        private final CircuitState.Value value;

        /**
         * Creates a literal with the given circuit state value.
         *
         * @param value the circuit state
         */
        Literal(CircuitState.Value value) {
            this.value = value;
        }

        @Override
        public CircuitState.Value value() {
            return value;
        }
    }
}
