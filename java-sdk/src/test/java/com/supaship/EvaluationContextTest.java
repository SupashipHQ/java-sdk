package com.supaship;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvaluationContextTest {

    @Test
    void of_singleKey() {
        EvaluationContext ec = EvaluationContext.of("region", "eu");
        assertEquals(Map.of("region", "eu"), ec.asMap());
    }

    @Test
    void builder_userIdAndAttributes() {
        EvaluationContext ec =
                EvaluationContext.builder()
                        .userId("user_42")
                        .attribute("region", "eu")
                        .attribute("plan", "pro")
                        .build();
        assertEquals(
                Map.of("userId", "user_42", "region", "eu", "plan", "pro"), ec.asMap());
    }

    @Test
    void builder_rejectsNullAttributeKey() {
        assertThrows(
                NullPointerException.class,
                () -> EvaluationContext.builder().attribute(null, "v").build());
    }

    @Test
    void configBuilder_acceptsEvaluationContext() {
        SupashipClientConfig cfg =
                SupashipClientConfig.builder()
                        .sdkKey("k")
                        .environment("e")
                        .defaults(FeatureDefaults.builder().feature("f", true).build())
                        .context(EvaluationContext.of("region", "eu"))
                        .build();
        assertEquals(Map.of("region", "eu"), cfg.context());
    }
}
