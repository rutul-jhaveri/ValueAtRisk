package com.var.calculation.model.dto;

/**
 * Authentication response containing JWT token and user details.
 * Uses Java 21 record for immutability.
 *
 * @param token JWT authentication token
 * @param role User's role (USER or ADMIN)
 * @param expiresIn Token expiration time in seconds
 */
public record AuthResponse(
    String token,
    String role,
    Long expiresIn
) {}
