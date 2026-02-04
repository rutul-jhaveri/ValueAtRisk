package com.var.calculation.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Authentication request containing user credentials.
 * Uses Java 21 record for immutability.
 *
 * @param username User's username
 * @param password User's password
 */
public record AuthRequest(
    @NotBlank(message = "Username is required")
    String username,
    
    @NotBlank(message = "Password is required")
    String password
) {}
