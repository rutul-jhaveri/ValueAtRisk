package com.var.calculation.service;

import com.var.calculation.model.dto.*;
import com.var.calculation.strategy.HistoricalSimulationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VarCalculationService {
    
    private final HistoricalSimulationStrategy strategy;
    private final AuditService auditService;
    
    @Cacheable(value = "tradeVarCache", key = "#request.tradeId() + '_' + #request.confidenceLevel()")
    public VarResponse calculateTradeVaR(TradeVarRequest request, String username) {
        log.debug("Calculating VaR for trade: {} by user: {}", request.tradeId(), username);
        
        long startTime = System.currentTimeMillis();
        
        try {
            double var = strategy.calculateTradeVaR(request.historicalPnL(), request.confidenceLevel());
            
            auditService.logRequest(username, "/api/v1/var/trade", 
                System.currentTimeMillis() - startTime, true, null);
            
            return new VarResponse(
                request.tradeId(),
                var,
                request.confidenceLevel(),
                "HISTORICAL_SIMULATION",
                1,
                LocalDateTime.now()
            );
        } catch (Exception e) {
            auditService.logRequest(username, "/api/v1/var/trade", 
                System.currentTimeMillis() - startTime, false, e.getMessage());
            log.error("VaR calculation failed for trade: {}", request.tradeId(), e);
            throw e;
        }
    }
    
    @Cacheable(value = "portfolioVarCache", key = "#request.portfolioId() + '_' + #request.confidenceLevel()")
    public VarResponse calculatePortfolioVaR(PortfolioVarRequest request, String username) {
        log.debug("Calculating portfolio VaR: {} with {} trades by user: {}", 
            request.portfolioId(), request.trades().size(), username);
        
        long startTime = System.currentTimeMillis();
        
        try {
            var tradesPnL = request.trades().stream()
                .map(Trade::historicalPnL)
                .toList();
            
            double var = strategy.calculatePortfolioVaR(tradesPnL, request.confidenceLevel());
            
            auditService.logRequest(username, "/api/v1/var/portfolio", 
                System.currentTimeMillis() - startTime, true, null);
            
            return new VarResponse(
                request.portfolioId(),
                var,
                request.confidenceLevel(),
                "HISTORICAL_SIMULATION",
                request.trades().size(),
                LocalDateTime.now()
            );
        } catch (Exception e) {
            auditService.logRequest(username, "/api/v1/var/portfolio", 
                System.currentTimeMillis() - startTime, false, e.getMessage());
            log.error("Portfolio VaR calculation failed: {}", request.portfolioId(), e);
            throw e;
        }
    }
}
