package com.srh.cinepass.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("Too many verification attempts. Please wait a moment and try again.");
    }
}
