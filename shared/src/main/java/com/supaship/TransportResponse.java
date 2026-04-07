package com.supaship;

/** Minimal HTTP result for {@link EvaluateTransport}. */
public final class TransportResponse {

    private final int statusCode;
    private final String body;

    public TransportResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body != null ? body : "";
    }

    public int statusCode() {
        return statusCode;
    }

    public String body() {
        return body;
    }
}
