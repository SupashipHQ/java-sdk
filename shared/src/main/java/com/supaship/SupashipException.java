package com.supaship;

/** Unchecked exception for HTTP or SDK failures that callers may inspect or wrap. */
public final class SupashipException extends RuntimeException {

    /** HTTP status when the failure was due to a non-2xx response; otherwise {@code null}. */
    private final Integer httpStatus;

    /**
     * Failure without an associated HTTP status (for example parsing or internal SDK errors).
     *
     * @param message error description
     */
    public SupashipException(String message) {
        super(message);
        this.httpStatus = null;
    }

    /**
     * Failure with a root cause and no HTTP status.
     *
     * @param message error description
     * @param cause   underlying cause
     */
    public SupashipException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = null;
    }

    /**
     * Failure caused by a non-success HTTP response from the Supaship API.
     *
     * @param httpStatus HTTP status code returned by the API
     * @param message    error description
     */
    public SupashipException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /**
     * Optional HTTP status attached by {@link #SupashipException(int, String)}.
     *
     * @return HTTP status when constructed with {@link #SupashipException(int, String)}; otherwise {@code null}
     */
    public Integer httpStatus() {
        return httpStatus;
    }
}
