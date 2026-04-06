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
 * Annotation that defines the number of successful executions needed for
 * the circuit-breaker to transition from half-open to closed.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SuccessThreshold {

    /**
     * The number of consecutive successes required to close the circuit.
     *
     * @return the success count
     */
    int value();

    /** Default literal requiring 3 successes. */
    Literal DEFAULT = new Literal();

    /**
     * Annotation literal for programmatic use of {@link SuccessThreshold}.
     */
    class Literal extends AnnotationLiteral<SuccessThreshold> implements SuccessThreshold {

        private static final long serialVersionUID = 7310730593030223981L;

        private final int value;

        private Literal() {
            value = 3;
        }

        /**
         * Creates a literal with the given success count.
         *
         * @param value the success count
         */
        Literal(int value) {
            this.value = value;
        }

        @Override
        public int value() {
            return value;
        }
    }
}
