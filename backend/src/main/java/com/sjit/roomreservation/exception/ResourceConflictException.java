package com.sjit.roomreservation.exception;

/** Custom exception for resource conflict errors (e.g., double-booking). */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}