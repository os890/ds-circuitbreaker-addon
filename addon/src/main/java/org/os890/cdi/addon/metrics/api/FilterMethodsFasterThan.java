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

import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.*;

/**
 * [optional]
 * Allows to skip metrics-details for methods which execute faster than the given value.
 * Furthermore OverloadProtectionInterceptor filters executions faster than 2ms to improve the overall perf.
 * (to change that see the ds-config-key: OverloadProtection_filterMethodsFasterThanMs)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface FilterMethodsFasterThan {
    int ms(); //use @OverloadProtection(collectMetrics = false) to skip metrics-recording at all

    Literal DEFAULT = new Literal();

    class Literal extends AnnotationLiteral<FilterMethodsFasterThan> implements FilterMethodsFasterThan {
        private static final long serialVersionUID = 7310730593030223981L;

        private final int value;

        private Literal() {
            String configuredValue = ConfigResolver.getProjectStageAwarePropertyValue(FilterMethodsFasterThan.class.getSimpleName() + "_ms", "100");
            value = Integer.parseInt(configuredValue);
        }

        public Literal(int value) {
            this.value = value;
        }

        @Override
        public int ms() {
            return value;
        }
    }
}
