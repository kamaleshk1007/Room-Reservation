package com.sjit.roomreservation.exception;

/**
 * Raised when a hand-off token arrives a second time. Hand-off tokens are single use.
 */
public class ReplayedHandoffTokenException extends RuntimeException {

    public ReplayedHandoffTokenException(String message) {
        super(message);
    }
}
