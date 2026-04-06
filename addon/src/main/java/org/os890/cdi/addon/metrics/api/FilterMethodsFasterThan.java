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

package org.os890.cdi.addon.metrics.api;

import org.apache.deltaspike.core.api.config.ConfigResolver;

import jakarta.enterprise.util.AnnotationLiteral;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional annotation that allows skipping metrics details for methods which
 * execute faster than the given value.
 *
 * <p>Furthermore {@link org.os890.cdi.addon.circuitbreaker.impl.OverloadProtectionInterceptor}
 * filters executions faster than 2ms to improve the overall performance.
 * To change that see the DeltaSpike config key:
 * {@code OverloadProtection_filterMethodsFasterThanMs}.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface FilterMethodsFasterThan {

    /**
     * The minimum execution time in milliseconds.
     * Use {@code @OverloadProtection(collectMetrics = false)} to skip metrics recording entirely.
     *
     * @return the threshold in milliseconds
     */
    int ms();

    /** Default literal with value from DeltaSpike configuration (default 100ms). */
    Literal DEFAULT = new Literal();

    /**
     * Annotation literal for programmatic use of {@link FilterMethodsFasterThan}.
     */
    class Literal extends AnnotationLiteral<FilterMethodsFasterThan> implements FilterMethodsFasterThan {

        private static final long serialVersionUID = 7310730593030223981L;

        private final int value;

        private Literal() {
            String configuredValue = ConfigResolver.getProjectStageAwarePropertyValue(
                    FilterMethodsFasterThan.class.getSimpleName() + "_ms", "100");
            value = Integer.parseInt(configuredValue);
        }

        /**
         * Creates a literal with the given threshold.
         *
         * @param value the threshold in milliseconds
         */
        Literal(int value) {
            this.value = value;
        }

        @Override
        public int ms() {
            return value;
        }
    }
}
