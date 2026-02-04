package com.var.calculation.model.dto;

import com.var.calculation.validation.MinDataPoints;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Individual trade data for portfolio VaR calculation.
 * Uses Java 21 record for immutability.
 *
 * @param tradeId Unique identifier for the trade
 * @param historicalPnL Historical profit and loss data (configurable minimum points)
 */
public record Trade(
    @NotBlank(message = "Trade ID is required")
    String tradeId,
    
    @NotNull(message = "Historical P&L is required")
    @MinDataPoints
    List<Double> historicalPnL
) {}
