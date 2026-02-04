package com.var.calculation.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Request for calculating portfolio Value at Risk.
 * Uses Java 21 record for immutability.
 *
 * @param portfolioId Unique identifier for the portfolio
 * @param confidenceLevel Confidence level between 0 and 1 (e.g., 0.95 for 95%)
 * @param trades List of trades in the portfolio
 */
public record PortfolioVarRequest(
    @NotBlank(message = "Portfolio ID is required")
    String portfolioId,
    
    @NotNull(message = "Confidence level is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Confidence level must be greater than 0")
    @DecimalMax(value = "1.0", inclusive = false, message = "Confidence level must be less than 1")
    Double confidenceLevel,
    
    @NotNull(message = "Trades are required")
    @Size(min = 1, message = "At least one trade required")
    @Valid
    List<Trade> trades
) {}
