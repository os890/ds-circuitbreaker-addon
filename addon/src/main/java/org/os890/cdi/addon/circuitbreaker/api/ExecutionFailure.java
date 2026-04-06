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
import java.util.concurrent.TimeUnit;

/**
 * Optional annotation that defines the time after which an execution should be
 * marked as failed (without interruption).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ExecutionFailure {

    /**
     * The timeout duration value after which the execution is considered failed.
     *
     * @return the timeout amount
     */
    int after();

    /**
     * The time unit for the timeout.
     *
     * @return the time unit
     */
    TimeUnit timeUnit();

    /** Default literal with a 1-second timeout. */
    Literal DEFAULT = new Literal();

    /**
     * Annotation literal for programmatic use of {@link ExecutionFailure}.
     */
    class Literal extends AnnotationLiteral<ExecutionFailure> implements ExecutionFailure {

        private static final long serialVersionUID = 7310730593030223981L;

        private final int after;
        private final TimeUnit timeUnit;

        private Literal() {
            after = 1;
            timeUnit = TimeUnit.SECONDS;
        }

        /**
         * Creates a literal with the given timeout and time unit.
         *
         * @param after    the timeout amount
         * @param timeUnit the time unit
         */
        Literal(int after, TimeUnit timeUnit) {
            this.after = after;
            this.timeUnit = timeUnit;
        }

        @Override
        public int after() {
            return after;
        }

        @Override
        public TimeUnit timeUnit() {
            return timeUnit;
        }
    }
}
