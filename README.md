# Supaship Java SDK

Small, production oriented client for [Supaship](https://supaship.com) feature flags. It mirrors the public behavior of [`@supashiphq/javascript-sdk`](https://www.npmjs.com/package/@supashiphq/javascript-sdk) (`SupaClient`): same default endpoints, request shape, retry policy, timeouts, sensitive-context hashing, and fallback rules when the API is unavailable.

**Runtime:** Java 11 or newer. **Dependencies:** [Gson](https://github.com/google/gson) only (JSON). HTTP uses `java.net.http`.

Browser only features from the JS SDK (for example the toolbar plugin) are not applicable here.

## Install

### Maven

```xml
<dependency>
  <groupId>com.supaship</groupId>
  <artifactId>supaship-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

Replace the version with the latest release you publish (see [publish.md](publish.md) if you are publishing this library yourself).

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.supaship:supaship-sdk:1.0.0")
}
```

## Quick start

```java
import com.supaship.SupaClient;
import com.supaship.SupaClientConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Example {
  public static void main(String[] args) throws ExecutionException, InterruptedException {
    Map<String, Object> fallbacks =
        Map.of(
            "dark-mode", false,
            "max-items", 10L);

    SupaClient client =
        new SupaClient(
            SupaClientConfig.builder()
                .sdkKey(System.getenv("SUPASHIP_SDK_KEY"))
                .environment("production")
                .features(fallbacks)
                .context(Map.of("region", "eu"))
                .build());

    boolean dark =
        (Boolean) client.getFeature("dark-mode").get();

    Map<String, Object> batch =
        client.getFeatures(List.of("dark-mode", "max-items")).get();

    client.updateContext(Map.of("plan", "pro"), true);
  }
}
```

### Async API

All network calls return `CompletableFuture`. Use `thenApply`, `whenComplete`, or block with `get()` / `join()` as appropriate.

```java
client
    .getFeature("dark-mode", Map.of("userId", "u-123"))
    .thenAccept(
        value -> {
          // value is the flag value or the configured fallback type
        });
```

## Configuration

| Area | Java type | Notes |
|------|-----------|--------|
| SDK key | `SupaClientConfig.Builder.sdkKey` | Same as JS `sdkKey`; sent as `Authorization: Bearer …`. |
| Environment | `environment` | Same as JS `environment` (for example `production`, `staging`). |
| Fallbacks | `features(Map<String, Object>)` | Same role as JS `features` / `FeaturesWithFallbacks`. Used when the API fails or a variation is absent. Values may be `Boolean`, `Number`, `String`, `List`, `Map`, or `null`. |
| Default context | `context` | Merged into every evaluation unless you pass a per-call override. |
| Sensitive fields | `sensitiveContextProperties(Set<String>)` | Names of context keys whose values are replaced with a **SHA-256** hex digest before the request is sent (same idea as the JS client). |
| Network | `NetworkConfig` | Optional: `featuresApiUrl`, `eventsApiUrl` (reserved for future use), `retry`, `requestTimeout`, `HttpClient`. |

Defaults match the JS SDK:

- Features URL: `https://edge.supaship.com/v1/features`
- Events URL: `https://edge.supaship.com/v1/events` (not used by evaluate yet; kept for parity)
- Retry: enabled, 3 attempts, base backoff 1000 ms, exponential factor \(2^{attempt-1}\)
- Request timeout: 10 seconds

### Custom `HttpClient`

Use your own `java.net.http.HttpClient` for TLS settings, proxy, or HTTP version:

```java
import com.supaship.NetworkConfig;
import com.supaship.SupaClientConfig;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

HttpClient http =
    HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .version(HttpClient.Version.HTTP_1_1)
        .build();

NetworkConfig network =
    NetworkConfig.builder()
        .httpClient(http)
        .featuresApiUrl("https://edge.supaship.com/v1/features")
        .build();

SupaClientConfig config =
    SupaClientConfig.builder()
        .sdkKey(key)
        .environment("production")
        .features(Map.of("flag", false))
        .networkConfig(network)
        .build();
```

### Listeners

`SupaClientListener` provides optional hooks similar in spirit to JS plugins (before/after fetch, retries, errors, fallbacks, context updates). Implement only what you need; default methods are no-ops.

```java
import com.supaship.SupaClientConfig;
import com.supaship.SupaClientListener;

import java.util.Map;

SupaClientConfig config =
    SupaClientConfig.builder()
        .sdkKey(key)
        .environment("production")
        .features(Map.of("flag", false))
        .addListener(
            new SupaClientListener() {
              @Override
              public void onError(Throwable error, Map<String, ?> context) {
                // log, metrics, etc.
              }
            })
        .build();
```

## Spring Boot

Use a single application scoped `SupaClient` bean and inject it where needed. Prefer configuration properties for the SDK key and environment.

### `application.properties`

```properties
supaship.sdk-key=${SUPASHIP_SDK_KEY}
supaship.environment=production
```

### Configuration bean

```java
import com.supaship.NetworkConfig;
import com.supaship.SupaClient;
import com.supaship.SupaClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(SupashipConfiguration.SupashipProps.class)
public class SupashipConfiguration {

  @Bean
  public SupaClient supaClient(SupashipProps props) {
    Map<String, Object> features =
        Map.of(
            "new-checkout", false,
            "banner-message", "Welcome");

    SupaClientConfig config =
        SupaClientConfig.builder()
            .sdkKey(props.getSdkKey())
            .environment(props.getEnvironment())
            .features(features)
            .context(Map.of("service", "api"))
            .networkConfig(NetworkConfig.builder().build())
            .build();

    return new SupaClient(config);
  }

  @ConfigurationProperties(prefix = "supaship")
  public static class SupashipProps {
    private String sdkKey;
    private String environment;

    public String getSdkKey() {
      return sdkKey;
    }

    public void setSdkKey(String sdkKey) {
      this.sdkKey = sdkKey;
    }

    public String getEnvironment() {
      return environment;
    }

    public void setEnvironment(String environment) {
      this.environment = environment;
    }
  }
}
```

### Using the client in a service

```java
import com.supaship.SupaClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FeatureService {
  private final SupaClient supaship;

  public FeatureService(SupaClient supaship) {
    this.supaship = supaship;
  }

  public boolean isNewCheckoutEnabled(String userId) throws Exception {
    Object v = supaship.getFeature("new-checkout", Map.of("userId", userId)).get();
    return Boolean.TRUE.equals(v);
  }
}
```

**Gradle note:** Spring Boot does not change how you add this SDK: use `implementation("com.supaship:supaship-sdk:…")` alongside your usual `org.springframework.boot` dependencies.

## Quarkus / Micronaut / other frameworks

There is no framework specific integration: create one `SupaClient` instance (or one per tenant) at startup with `SupaClientConfig`, expose it as a CDI bean / singleton, and inject it into resources or services the same way as any other HTTP client.

## Error handling and fallbacks

If the features HTTP call fails after retries, or the response is not successful, `getFeature` / `getFeatures` still **complete normally** with values from your configured `features` map (fallbacks). That matches the JavaScript client.

Use `SupaClientListener.onError` and `onFallbackUsed` if you want metrics or logs when fallbacks are used.

Very rare failures (for example if SHA-256 is unavailable) complete the `CompletableFuture` exceptionally with `SupashipException`. Ordinary HTTP or parse errors follow the fallback path above instead of failing the future.

## Building from source

```bash
mvn test package
```

The resulting JAR is `target/supaship-sdk-*.jar`.

## JavaScript parity (summary)

| JavaScript `SupaClient` | Java `SupaClient` |
|------------------------|-------------------|
| `getFeature`, `getFeatures` | Same, return `CompletableFuture` |
| `updateContext`, `getContext` | Same |
| `getFeatureFallback` | `getFeatureFallback` |
| `networkConfig` (URLs, retry, timeout, custom fetch) | `NetworkConfig` + optional `HttpClient` |
| `sensitiveContextProperties` | Same (SHA-256 hex) |
| Plugins | `SupaClientListener` (subset of hooks) |
| Toolbar plugin | Not applicable on the JVM |

## License

See [LICENSE](LICENSE).