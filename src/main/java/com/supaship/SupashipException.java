package com.supaship;

/** Unchecked exception for HTTP or SDK failures that callers may inspect or wrap. */
public final class SupashipException extends RuntimeException {

    private final Integer httpStatus;

    public SupashipException(String message) {
        super(message);
        this.httpStatus = null;
    }

    public SupashipException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = null;
    }

    public SupashipException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /** Present when the failure came from a non-success HTTP status. */
    public Integer httpStatus() {
        return httpStatus;
    }
}
