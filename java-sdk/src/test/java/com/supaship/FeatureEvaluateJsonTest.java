package com.supaship;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureEvaluateJsonTest {

    @Test
    void buildEvaluateRequest_escapesKeysAndStrings() {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("a\"b", "line\nbreak");
        ctx.put("n", 1L);
        ctx.put("flag", true);
        ctx.put("nil", null);
        String json =
                FeatureEvaluateJson.buildEvaluateRequest("prod", List.of("feat-1", "f\"x"), ctx);
        assertTrue(json.contains("\"environment\":\"prod\""));
        assertTrue(json.contains("\"features\":[\"feat-1\",\"f\\\"x\"]"));
        assertTrue(json.contains("\\n"));
    }

    @Test
    void parseEvaluateResponse_readsVariations() {
        String json =
                "{\"features\":{\"dark\":{\"variation\":true},\"legacy\":{\"variation\":null},\"obj\":{\"variation\":{\"x\":1}}}}";
        Map<String, Object> m = FeatureEvaluateJson.parseEvaluateResponse(json);
        assertEquals(true, m.get("dark"));
        assertNull(m.get("legacy"));
        @SuppressWarnings("unchecked")
        Map<String, Object> inner = (Map<String, Object>) m.get("obj");
        assertEquals(1L, inner.get("x"));
    }

    @Test
    void parseEvaluateResponse_nestedObjectNumber() {
        String json = "{\"features\":{\"p\":{\"variation\":3.14}}}";
        assertEquals(3.14, (Double) FeatureEvaluateJson.parseEvaluateResponse(json).get("p"));
    }

    @Test
    void parseEvaluateResponse_arrayVariation() {
        String json = "{\"features\":{\"arr\":{\"variation\":[1,2,3]}}}";
        @SuppressWarnings("unchecked")
        List<Object> arr =
                (List<Object>) FeatureEvaluateJson.parseEvaluateResponse(json).get("arr");
        assertEquals(List.of(1L, 2L, 3L), arr);
    }

    @Test
    void roundTripBoolean() {
        assertFalse(
                (Boolean)
                        FeatureEvaluateJson.parseEvaluateResponse(
                                        "{\"features\":{\"f\":{\"variation\":false}}}")
                                .get("f"));
    }
}
