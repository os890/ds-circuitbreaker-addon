# DeltaSpike Circuit-Breaker Add-on

A CDI interceptor-based circuit-breaker implementation built on top of
[Failsafe](https://failsafe.dev/) and [Apache DeltaSpike](https://deltaspike.apache.org/).

## Overview

This add-on provides a `@OverloadProtection` interceptor binding that wraps
method calls with a Failsafe `CircuitBreaker`. When a protected method fails
repeatedly, the circuit opens and subsequent calls are rejected with a
`ServiceOverloadedException` until the service recovers.

### Features

- **Annotation-driven** circuit-breaker configuration (`@FailureThreshold`,
  `@SuccessThreshold`, `@CircuitOpenDelay`, `@ExecutionFailure`)
- **CDI event broadcasting** on circuit state changes (open, half-open, closed)
- **Built-in metrics collection** with per-second statistics (avg, min, max, percentiles)
- **DeltaSpike configuration** support for runtime tuning

## Architecture

```
api/              Annotations and event types
  CircuitEvent, CircuitState, OverloadProtection, ...
impl/             CDI interceptor and circuit-breaker provider
  OverloadProtectionInterceptor, CircuitBreakerProvider, ...
metrics/api/      Metrics annotations
  FilterMethodsFasterThan
metrics/impl/     Asynchronous metrics storage
  MetricsStorage, StatsEntry, AsyncMetricsCollector, ...
```

## Usage

Annotate a CDI bean method with `@OverloadProtection`:

```java
@ApplicationScoped
public class MyService {

    @OverloadProtection
    @FailureThreshold(failures = 5, executions = 10)
    @SuccessThreshold(3)
    @CircuitOpenDelay(delay = 5, timeUnit = TimeUnit.SECONDS)
    public String callExternalService() {
        // ...
    }
}
```

Observe state changes via CDI events:

```java
public void onOpen(@Observes @CircuitState(CircuitState.Value.OPEN) CircuitEvent event) {
    log.warn("Circuit opened for " + event.getMethodKey());
}
```

## Requirements

- Java 25+
- Maven 3.6.3+
- Jakarta CDI 4.1
- Failsafe 3.3.2
- DeltaSpike 2.0.0

## Quality Plugins

The build enforces:

- **Compiler**: `-Xlint:all`, `failOnWarning=true`
- **Enforcer**: Java 25+, Maven 3.6.3+, dependency convergence, banned javax dependencies
- **Checkstyle**: no star imports, braces required, whitespace rules
- **Apache RAT**: license header verification
- **JaCoCo**: code coverage reporting
- **Surefire**: test execution (pinned version)

## Building

```bash
mvn clean verify
```

## Testing

Tests use the [dynamic-cdi-test-bean-addon](https://github.com/os890/dynamic-cdi-test-bean-addon)
with `@EnableTestBeans` for CDI SE integration testing.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
