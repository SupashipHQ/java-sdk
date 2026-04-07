package com.supaship;

import org.jetbrains.annotations.NotNull;

/**
 * Platform network layer that can construct a {@link SupashipClient} (for example JVM
 * {@code NetworkConfig} or Android {@code AndroidSupashipNetwork}).
 */
public interface SupashipNetwork {

    /**
     * @throws IllegalArgumentException if {@code config.networkSettings()} does not match this network's settings
     */
    @NotNull
    SupashipClient client(@NotNull SupashipClientConfig config);
}
