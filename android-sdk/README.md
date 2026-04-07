# Supaship Android SDK (`supaship-android-sdk`)

JVM-compatible library (Java&nbsp;11 language level) for Android apps. Core types and `SupaClient` live under `../shared` and are compiled into this artifact with an `HttpURLConnection`-based [`AndroidEvaluateTransport`](src/main/java/com/supaship/AndroidEvaluateTransport.java).

Do heavy work off the main thread: evaluation returns `CompletableFuture` but uses your executor (default is `ForkJoinPool.commonPool()`).

## Dependency (Maven)

```xml
<dependency>
  <groupId>com.supaship</groupId>
  <artifactId>supaship-android-sdk</artifactId>
  <version>${supaship.version}</version>
</dependency>
```

## Kotlin (coroutines)

```kotlin
import com.supaship.AndroidSupaNetwork
import com.supaship.SupaClientConfig
import kotlinx.coroutines.future.await
import java.util.concurrent.Executors

val io = Executors.newCachedThreadPool() // or your own backing an OkHttp-only transport later

val net = AndroidSupaNetwork.builder()
    .executor(io)
    .build()

val config = SupaClientConfig.builder()
    .sdkKey(BuildConfig.SUPASHIP_SDK_KEY)
    .environment("production")
    .features(mapOf("new-onboarding" to false))
    .networkSettings(net.settings())
    .build()

val client: com.supaship.SupaClient = net.client(config)

suspend fun isOnboardingEnabled(): Boolean =
    client.getFeature("new-onboarding").await() as? Boolean ?: false
```

## Java

```java
import com.supaship.AndroidSupaNetwork;
import com.supaship.SupaClient;
import com.supaship.SupaClientConfig;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

ExecutorService io = Executors.newCachedThreadPool();

AndroidSupaNetwork net = AndroidSupaNetwork.builder().executor(io).build();

SupaClientConfig config =
        SupaClientConfig.builder()
                .sdkKey(System.getenv("SUPASHIP_SDK_KEY"))
                .environment("production")
                .features(Map.of("new-onboarding", false))
                .networkSettings(net.settings())
                .build();

SupaClient client = net.client(config);

client.getFeature("new-onboarding")
        .thenAccept(v -> {
            boolean on = Boolean.TRUE.equals(v);
            // update UI on main thread
        });
```

## Custom transport

If you prefer OkHttp or another stack, implement [`EvaluateTransport`](../shared/src/main/java/com/supaship/EvaluateTransport.java) and call `new SupaClient(config, yourTransport)`.

## JVM / server Java

Use the [`java-sdk`](../java-sdk) module with [`NetworkConfig`](../java-sdk/src/main/java/com/supaship/NetworkConfig.java) and `JavaEvaluateTransport` (not this artifact).
