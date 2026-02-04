package com.var.calculation.exception;

/**
 * Standard error response for API exceptions.
 * Uses Java 21 record for immutability.
 *
 * @param error Error type or category
 * @param message Detailed error message
 */
public record ErrorResponse(
    String error,
    String message
) {}
