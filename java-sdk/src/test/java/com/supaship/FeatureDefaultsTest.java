package com.supaship;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FeatureDefaultsTest {

    @Test
    void builder_collects_features() {
        FeatureDefaults d =
                FeatureDefaults.builder()
                        .feature("dark-mode", false)
                        .feature("max-items", 10L)
                        .feature("theme", "light")
                        .build();
        assertEquals(Map.of("dark-mode", false, "max-items", 10L, "theme", "light"), d.asMap());
    }

    @Test
    void rejectsUnsupportedType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> FeatureDefaults.builder().feature("x", new Object()));
    }

    @Test
    void rejectsBlankFeatureName() {
        assertThrows(IllegalArgumentException.class, () -> FeatureDefaults.builder().feature("  ", true));
    }

    @Test
    void nestedStructures() {
        FeatureDefaults d =
                FeatureDefaults.builder()
                        .feature("list", List.of(1L, "a", Map.of("k", true)))
                        .build();
        assertEquals(List.of(1L, "a", Map.of("k", true)), d.asMap().get("list"));
    }

    @Test
    void rejectsNonStringMapKeys() {
        Map<Object, String> bad = new java.util.HashMap<>();
        bad.put(1, "x");
        assertThrows(
                IllegalArgumentException.class, () -> FeatureDefaults.builder().feature("m", bad));
    }

    @Test
    void config_defaults_wiring() {
        FeatureDefaults d = FeatureDefaults.builder().feature("f", true).build();
        SupashipClientConfig cfg =
                SupashipClientConfig.builder()
                        .sdkKey("k")
                        .environment("e")
                        .defaults(d)
                        .build();
        assertEquals(Map.of("f", true), cfg.features());
    }
}
