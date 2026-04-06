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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional annotation that defines how many failures are acceptable before the
 * circuit-breaker opens.
 *
 * <p>The circuit opens when {@link #failures()} out of the last
 * {@link #executions()} invocations have failed.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface FailureThreshold {

    /**
     * The number of failures that trigger the circuit to open.
     *
     * @return the failure count
     */
    int failures();

    /**
     * The rolling window of executions to consider.
     *
     * @return the execution window size
     */
    int executions();

    /** Default literal with 8 failures out of 10 executions. */
    Literal DEFAULT = new Literal();

    /**
     * Annotation literal for programmatic use of {@link FailureThreshold}.
     */
    class Literal extends AnnotationLiteral<FailureThreshold> implements FailureThreshold {

        private static final long serialVersionUID = 7310730593030223981L;

        private final int failures;
        private final int executions;

        private Literal() {
            failures = 8;
            executions = 10;
        }

        /**
         * Creates a literal with the given failure and execution counts.
         *
         * @param failures   the failure count
         * @param executions the execution window size
         */
        Literal(int failures, int executions) {
            this.failures = failures;
            this.executions = executions;
        }

        @Override
        public int failures() {
            return failures;
        }

        @Override
        public int executions() {
            return executions;
        }
    }
}
