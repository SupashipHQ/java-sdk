# Supaship Java SDK

> Lightweight, production-ready Java client for [Supaship](https://supaship.com) feature flags.

**Minimum runtime:** Java 11+
**Dependencies:** [Gson](https://github.com/google/gson) · [JetBrains Annotations](https://github.com/JetBrains/java-annotations)
**HTTP:** `java.net.http` (no extra HTTP library needed)
**Kotlin:** Fully supported — see [Kotlin usage](#kotlin-usage)
**Android:** Use Maven artifact `android-sdk` — see [android-sdk/README.md](android-sdk/README.md) (shared sources are under `shared/`, not published as a separate coordinate).

---

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration Reference](#configuration-reference)
- [Async API](#async-api)
- [Listeners](#listeners)
- [Custom HttpClient](#custom-httpclient)
- [Framework Integration](#framework-integration)
  - [Spring Boot](#spring-boot)
  - [Quarkus / Micronaut](#quarkus--micronaut)
- [Kotlin Usage](#kotlin-usage)
- [Error Handling and Fallbacks](#error-handling-and-fallbacks)
- [Building from Source](#building-from-source)
- [License](#license)

---

## Installation

### Maven
```xml
<dependency>
  <groupId>com.supaship</groupId>
  <artifactId>java-sdk</artifactId>
  <version>VERSION</version>
</dependency>
```

### Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("com.supaship:java-sdk:VERSION")
}
```

### Gradle (Groovy DSL)
```groovy
dependencies {
    implementation 'com.supaship:java-sdk:VERSION'
}
```

Replace `VERSION` with the [latest release](https://central.sonatype.com/artifact/com.supaship/java-sdk).

---

## Quick Start
```java
import com.supaship.EvaluationContext;
import com.supaship.FeatureDefaults;
import com.supaship.SupashipClient;
import com.supaship.SupashipClientConfig;

import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws Exception {

        // 1. Fallbacks when the API is unreachable — Supaship variations are boolean or JSON only;
        //    use Boolean / null / String / Number / List / Map (validated when built).
        FeatureDefaults defaults =
            FeatureDefaults.builder()
                .feature("dark-mode", false)
                .feature("max-items", 10L)
                .feature("theme", "light")
                .build();

        // 2. Build config — the JVM module wires java.net.http automatically
        SupashipClientConfig config =
            SupashipClientConfig.builder()
                .sdkKey(System.getenv("SUPASHIP_SDK_KEY"))
                .environment("production")
                .defaults(defaults)
                .context(EvaluationContext.of("region", "eu"))
                // Or: .context(EvaluationContext.builder()
                //     .userId("user_42").attribute("region", "eu").attribute("plan", "pro").build())
                .build();
        SupashipClient client = SupashipClient.create(config);

        // Advanced: custom NetworkConfig — set config's networkSettings to match, then:
        // NetworkConfig network = NetworkConfig.builder()
        //     .proxy("http://proxy.corp.com")
        //     .retries(3)
        //     .build();
        // SupashipClientConfig config2 = SupashipClientConfig.builder()
        //     ...same as config, but .networkSettings(network.settings())...
        //     .build();
        // SupashipClient client2 = SupashipClient.create(config2, network);

        // 3. Evaluate a single flag (blocks until resolved)
        boolean darkMode = (Boolean) client.getFeature("dark-mode").get();

        // 4. Evaluate multiple flags in one call
        Map<String, Object> flags = client.getFeatures(
            List.of("dark-mode", "max-items", "theme")
        ).get();

        // 5. Update the evaluation context at runtime
        client.updateContext(Map.of("plan", "pro"), true /* merge with existing */);
    }
}
```

---

## Configuration Reference

Pass all options through `SupashipClientConfig.builder()`:

| Option | Type | Required | Description |
|---|---|---|---|
| `sdkKey` | `String` | ✅ | Your Supaship SDK key. Sent as `Authorization: Bearer …` |
| `environment` | `String` | ✅ | Target environment, e.g. `production`, `staging` |
| `defaults` | `FeatureDefaults` | — | **Preferred** fallbacks; Supaship allows **boolean** and **JSON** values only — checked when `FeatureDefaults` is built |
| `features` | `Map<String, Object>` | — | Untyped fallbacks (same type rules). Supply via `defaults` or `features` (typically one of them) |
| `context` | `EvaluationContext` | — | Default evaluation context; use `EvaluationContext.of(…)` or `EvaluationContext.builder()…build()` |
| `contextMap` | `Map<String, ?>` | — | Raw context entries when you already have a map |
| `sensitiveContextProperties` | `Set<String>` | — | Context keys whose values are hashed with SHA-256 before the request is sent |
| `networkSettings` | `NetworkSettings` | — | URLs, per-request timeout, and retry policy (shared with Android) |
| `addListener` | `SupashipClientListener` | — | Lifecycle hooks: errors, retries, fallbacks, context updates |

On the JVM, call [`SupashipClient.create(SupashipClientConfig)`](shared/src/main/java/com/supaship/SupashipClient.java) for the default `java.net.http` stack. For a customized [`NetworkConfig`](java-sdk/src/main/java/com/supaship/NetworkConfig.java) (proxy, retries, custom `HttpClient`, etc.), set `SupashipClientConfig.Builder#networkSettings` to `network.settings()` and use `SupashipClient.create(config, network)` or `network.client(config)`.

### Defaults

| Setting | Default |
|---|---|
| Features URL | `https://edge.supaship.com/v1/features` |
| Events URL | `https://edge.supaship.com/v1/events` |
| Request timeout | 10 seconds |
| Retry attempts | 3 |
| Retry base backoff | 1000 ms (exponential: `base × 2^(attempt-1)`) |

---

## Async API

Every network call returns a `CompletableFuture`. You can block, chain, or handle errors asynchronously:
```java
// Non-blocking — chain a callback
client.getFeature("dark-mode")
    .thenAccept(value -> System.out.println("dark-mode = " + value));

// Non-blocking with per-call context override
client.getFeature("dark-mode", Map.of("userId", "u-123"))
    .thenAccept(value -> renderUI((Boolean) value));

// Evaluate multiple flags non-blocking
client.getFeatures(List.of("dark-mode", "max-items"))
    .thenAccept(flags -> {
        boolean dark  = (Boolean) flags.get("dark-mode");
        long    limit = (Long)    flags.get("max-items");
    });

// Blocking (useful in tests or CLI tools)
boolean dark = (Boolean) client.getFeature("dark-mode").get();

// Blocking with timeout
boolean dark = (Boolean) client
    .getFeature("dark-mode")
    .get(5, TimeUnit.SECONDS);
```

---

## Listeners

`SupashipClientListener` provides optional lifecycle hooks. All methods have default no-op implementations — implement only what you need.
```java
import com.supaship.FeatureDefaults;
import com.supaship.SupashipClientConfig;
import com.supaship.SupashipClientListener;

SupashipClientConfig config = SupashipClientConfig.builder()
    .sdkKey(System.getenv("SUPASHIP_SDK_KEY"))
    .environment("production")
    .defaults(FeatureDefaults.builder().feature("dark-mode", false).build())
    .addListener(new SupashipClientListener() {

        @Override
        public void onError(Throwable error, Map<String, ?> context) {
            // Log or report errors
            logger.error("Supaship error: {}", error.getMessage());
        }

        @Override
        public void onFallbackUsed(String feature, Object fallbackValue) {
            // Track when fallbacks are served — useful for metrics
            metrics.increment("supaship.fallback", "feature:" + feature);
        }

        @Override
        public void onRetry(int attempt, Throwable cause) {
            logger.warn("Supaship retry attempt {}: {}", attempt, cause.getMessage());
        }
    })
    .build();
```

---

## Custom HttpClient

Use your own `java.net.http.HttpClient` to control TLS settings, proxy, connection pool, or HTTP version:
```java
import com.supaship.FeatureDefaults;
import com.supaship.NetworkConfig;
import com.supaship.SupashipClient;
import com.supaship.SupashipClientConfig;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

HttpClient http = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(5))
    .version(HttpClient.Version.HTTP_2)
    .proxy(ProxySelector.of(new InetSocketAddress("proxy.internal", 8080)))
    .build();

NetworkConfig network = NetworkConfig.builder()
    .httpClient(http)
    .requestTimeout(Duration.ofSeconds(8))
    .featuresApiUrl("https://edge.supaship.com/v1/features")
    .build();

SupashipClientConfig config =
    SupashipClientConfig.builder()
        .sdkKey(System.getenv("SUPASHIP_SDK_KEY"))
        .environment("production")
        .defaults(FeatureDefaults.builder().feature("dark-mode", false).build())
        .networkSettings(network.settings())
        .build();
SupashipClient client = SupashipClient.create(config, network);
```

---

## Framework Integration

### Spring Boot

Declare a single application-scoped `SupashipClient` bean and inject it wherever needed.

#### `application.properties`
```properties
supaship.sdk-key=${SUPASHIP_SDK_KEY}
supaship.environment=production
```

#### Configuration class
```java
import com.supaship.EvaluationContext;
import com.supaship.FeatureDefaults;
import com.supaship.SupashipClient;
import com.supaship.SupashipClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SupashipConfiguration.SupashipProps.class)
public class SupashipConfiguration {

    @Bean
    public SupashipClient supashipClient(SupashipProps props) {
        SupashipClientConfig config =
            SupashipClientConfig.builder()
                .sdkKey(props.getSdkKey())
                .environment(props.getEnvironment())
                .defaults(
                    FeatureDefaults.builder()
                        .feature("new-checkout", false)
                        .feature("banner-message", "Welcome")
                        .build())
                .context(EvaluationContext.of("service", "api"))
                .build();
        return SupashipClient.create(config);
    }

    @ConfigurationProperties(prefix = "supaship")
    public static class SupashipProps {
        private String sdkKey;
        private String environment;

        public String getSdkKey()                   { return sdkKey; }
        public void setSdkKey(String sdkKey)        { this.sdkKey = sdkKey; }
        public String getEnvironment()              { return environment; }
        public void setEnvironment(String env)      { this.environment = env; }
    }
}
```

#### Injecting into a service
```java
import com.supaship.SupashipClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CheckoutService {

    private final SupashipClient supaship;

    public CheckoutService(SupashipClient supaship) {
        this.supaship = supaship;
    }

    public boolean isNewCheckoutEnabled(String userId) throws Exception {
        Object value = supaship
            .getFeature("new-checkout", Map.of("userId", userId))
            .get();
        return Boolean.TRUE.equals(value);
    }
}
```

---

### Quarkus / Micronaut

No framework-specific integration is required. Create one `SupashipClient` instance at startup and expose it as a CDI bean or singleton:
```java
// Quarkus — ApplicationScoped CDI bean
import com.supaship.FeatureDefaults;
import com.supaship.SupashipClient;
import com.supaship.SupashipClientConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class SupashipProducer {

    @Produces
    @ApplicationScoped
    public SupashipClient supashipClient() {
        SupashipClientConfig config =
            SupashipClientConfig.builder()
                .sdkKey(System.getenv("SUPASHIP_SDK_KEY"))
                .environment("production")
                .defaults(FeatureDefaults.builder().feature("feature-x", false).build())
                .build();
        return SupashipClient.create(config);
    }
}
```

Then inject `SupashipClient` into any resource or service with `@Inject`.

---

## Kotlin Usage

The SDK works with any JVM Kotlin project. Public API methods are annotated with `@NotNull` / `@Nullable` for Kotlin null-safety. Ensure your JVM target is **11 or newer**:
```kotlin
// build.gradle.kts
kotlin {
    jvmToolchain(11)
}
```

#### Blocking (no coroutines needed)
```kotlin
import com.supaship.EvaluationContext
import com.supaship.FeatureDefaults
import com.supaship.SupashipClient
import com.supaship.SupashipClientConfig

val config =
    SupashipClientConfig.builder()
        .sdkKey(System.getenv("SUPASHIP_SDK_KEY"))
        .environment("production")
        .defaults(
            FeatureDefaults.builder()
                .feature("dark-mode", false)
                .feature("max-items", 10L)
                .build())
        .context(EvaluationContext.of("region", "eu"))
        .build()
val client = SupashipClient.create(config)

val darkMode = client.getFeature("dark-mode").get() as Boolean
val flags    = client.getFeatures(listOf("dark-mode", "max-items")).get()
```

#### With coroutines

Add to your dependencies:
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.1")
```
```kotlin
import com.supaship.FeatureDefaults
import com.supaship.SupashipClient
import com.supaship.SupashipClientConfig
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val config =
        SupashipClientConfig.builder()
            .sdkKey(System.getenv("SUPASHIP_SDK_KEY"))
            .environment("production")
            .defaults(
                FeatureDefaults.builder()
                    .feature("dark-mode", false)
                    .feature("max-items", 10L)
                    .build())
            .build()
    val client = SupashipClient.create(config)

    val darkMode = client.getFeature("dark-mode").await() as Boolean
    val flags    = client.getFeatures(listOf("dark-mode", "max-items")).await()

    client.updateContext(mapOf("plan" to "pro"), mergeWithExisting = true)
}
```

---

## Error Handling and Fallbacks

The SDK is designed to never block your application if the Supaship API is unavailable.

- If a request fails after all retry attempts, `getFeature` / `getFeatures` **complete normally** with the values from your configured `features` fallback map — no exception is thrown.
- Use `SupashipClientListener.onError` and `onFallbackUsed` to observe when fallbacks are active.
- In rare cases where the JVM itself is in a degraded state (e.g. SHA-256 unavailable), the `CompletableFuture` completes exceptionally with `SupashipException`. Normal HTTP and parsing errors always follow the fallback path.
```java
client.getFeature("dark-mode")
    .whenComplete((value, error) -> {
        if (error != null) {
            // Only SupashipException reaches here — handle critical failures
            logger.error("Critical SDK failure", error);
        } else {
            // value is always non-null; may be the fallback
            renderUI((Boolean) value);
        }
    });
```

---

## Building from Source
```bash
git clone https://github.com/SupashipHQ/java-sdk.git
cd java-sdk
mvn verify
```

This compiles, runs tests, and packages the jar. No external services are required to build.

---

## License

[MIT](LICENSE)
