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

import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.*;

/**
 * Number of successful executions which are needed that the circuit-breaker transitions from half-open to closed
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SuccessThreshold {
    int value();

    class Literal extends AnnotationLiteral<SuccessThreshold> implements SuccessThreshold {
        private static final long serialVersionUID = 7310730593030223981L;

        private final int value;

        public Literal() {
            value = 3;
        }

        public Literal(int value) {
            this.value = value;
        }

        @Override
        public int value() {
            return value;
        }
    }
}
