package com.var.calculation.controller;

import com.var.calculation.model.dto.*;
import com.var.calculation.service.VarCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/var")
@RequiredArgsConstructor
@Tag(name = "VaR Calculation", description = "Value at Risk calculation endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class VarController {
    
    private final VarCalculationService varService;
    
    @PostMapping("/trade")
    @Operation(summary = "Calculate VaR for a single trade")
    public ResponseEntity<VarResponse> calculateTradeVaR(
            @Valid @RequestBody TradeVarRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(varService.calculateTradeVaR(request, authentication.getName()));
    }
    
    @PostMapping("/portfolio")
    @Operation(summary = "Calculate VaR for a portfolio")
    public ResponseEntity<VarResponse> calculatePortfolioVaR(
            @Valid @RequestBody PortfolioVarRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(varService.calculatePortfolioVaR(request, authentication.getName()));
    }
}
