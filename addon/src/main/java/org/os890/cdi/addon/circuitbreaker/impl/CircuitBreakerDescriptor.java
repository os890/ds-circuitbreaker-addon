package org.os890.cdi.addon.circuitbreaker.impl;

import java.lang.reflect.Method;

public class CircuitBreakerDescriptor {
    private final String key;
    private final Method currentMethod;

    public CircuitBreakerDescriptor(String key, Method currentMethod) {
        this.key = key;
        this.currentMethod = currentMethod;
    }

    public String getKey() {
        return key;
    }

    public Method getCurrentMethod() {
        return currentMethod;
    }
}
