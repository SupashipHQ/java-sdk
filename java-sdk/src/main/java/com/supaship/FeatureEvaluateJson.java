package com.supaship;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gson helpers for the Supaship <em>features evaluate</em> JSON request body and response envelope.
 *
 * <p>Request shape: {@code { "environment", "features": [...], "context": { ... } }}.
 * Response shape: {@code { "features": { "name": { "variation": ... } } }}.
 */
final class FeatureEvaluateJson {

    private static final Gson GSON =
            new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

    private FeatureEvaluateJson() {}

    /**
     * @param environment   environment name
     * @param featureNames  list of feature keys to evaluate
     * @param context       context object (may contain hashed sensitive fields)
     * @return JSON POST body for the evaluate endpoint
     */
    static String buildEvaluateRequest(
            String environment, List<String> featureNames, Map<String, Object> context) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("environment", environment);
        body.put("features", featureNames);
        body.put("context", context);
        return GSON.toJson(body);
    }

    /**
     * Parses {@code {"features":{"f":{"variation":...}}}} and returns a map of feature name to variation value
     * (booleans, strings, numbers, lists, or nested maps as plain Java objects).
     *
     * @param json raw response body from a successful evaluate call
     * @return ordered map of feature name to variation or {@code null} entries where the payload omits {@code variation}
     * @throws IllegalArgumentException if the JSON root or {@code features} object is missing or malformed
     * @throws JsonParseException        if the string is not valid JSON (propagated from Gson)
     */
    static Map<String, Object> parseEvaluateResponse(String json) {
        JsonElement rootEl;
        try {
            rootEl = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            throw e;
        }
        if (!rootEl.isJsonObject()) {
            throw new IllegalArgumentException("Root must be a JSON object");
        }
        JsonObject root = rootEl.getAsJsonObject();
        if (!root.has("features") || !root.get("features").isJsonObject()) {
            throw new IllegalArgumentException("Missing or invalid 'features' object");
        }
        JsonObject features = root.getAsJsonObject("features");
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : features.keySet()) {
            JsonElement entry = features.get(key);
            if (entry != null && entry.isJsonObject()) {
                JsonObject node = entry.getAsJsonObject();
                if (node.has("variation")) {
                    out.put(key, toJava(node.get("variation")));
                } else {
                    out.put(key, null);
                }
            } else {
                out.put(key, null);
            }
        }
        return out;
    }

    private static Object toJava(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return null;
        }
        if (el.isJsonPrimitive()) {
            return primitiveToJava(el.getAsJsonPrimitive());
        }
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            List<Object> list = new ArrayList<>(arr.size());
            for (JsonElement item : arr) {
                list.add(toJava(item));
            }
            return list;
        }
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            Map<String, Object> map = new LinkedHashMap<>();
            for (String k : obj.keySet()) {
                map.put(k, toJava(obj.get(k)));
            }
            return map;
        }
        return null;
    }

    private static Object primitiveToJava(JsonPrimitive p) {
        if (p.isBoolean()) {
            return p.getAsBoolean();
        }
        if (p.isString()) {
            return p.getAsString();
        }
        if (p.isNumber()) {
            String s = p.getAsNumber().toString();
            if (s.contains(".") || s.toLowerCase().contains("e")) {
                return p.getAsDouble();
            }
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return p.getAsDouble();
            }
        }
        return null;
    }
}
