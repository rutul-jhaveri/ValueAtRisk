package com.var.calculation.model.dto;

import java.time.LocalDateTime;

public record VarResponse(
    String id,
    Double var,
    Double confidenceLevel,
    String calculationMethod,
    Integer tradeCount,
    LocalDateTime timestamp
) {}
