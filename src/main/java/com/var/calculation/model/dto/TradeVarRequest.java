package com.var.calculation.model.dto;

import com.var.calculation.validation.MinDataPoints;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Request for calculating Value at Risk for a single trade.
 * Uses Java 21 record for immutability and conciseness.
 *
 * @param tradeId Unique identifier for the trade
 * @param historicalPnL Historical profit and loss data (configurable minimum points)
 * @param confidenceLevel Confidence level between 0 and 1 (e.g., 0.95 for 95%)
 */
public record TradeVarRequest(
    @NotBlank(message = "Trade ID is required")
    String tradeId,
    
    @NotNull(message = "Historical P&L is required")
    @MinDataPoints
    List<Double> historicalPnL,
    
    @NotNull(message = "Confidence level is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Confidence level must be greater than 0")
    @DecimalMax(value = "1.0", inclusive = false, message = "Confidence level must be less than 1")
    Double confidenceLevel
) {}
