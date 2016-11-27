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
import java.util.concurrent.TimeUnit;

/**
 * [optional]
 * how long the circuit should be open so that the application can recover
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface CircuitOpenDelay {
    int delay();
    TimeUnit timeUnit();

    Literal DEFAULT = new Literal();

    class Literal extends AnnotationLiteral<CircuitOpenDelay> implements CircuitOpenDelay {
        private static final long serialVersionUID = 7310730593030223981L;

        private final int delay;
        private final TimeUnit timeUnit;

        private Literal() {
            delay = 3;
            timeUnit = TimeUnit.SECONDS;
        }

        public Literal(int delay, TimeUnit timeUnit) {
            this.delay = delay;
            this.timeUnit = timeUnit;
        }

        @Override
        public int delay() {
            return delay;
        }

        @Override
        public TimeUnit timeUnit() {
            return timeUnit;
        }
    }
}
