package com.var.calculation.service;

import com.var.calculation.model.dto.*;
import com.var.calculation.strategy.HistoricalSimulationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for VarCalculationService.
 * Follows TDD principles with clear test structure and assertions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VarCalculationService Tests")
class VarCalculationServiceTest {
    
    @Mock
    private HistoricalSimulationStrategy strategy;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private VarCalculationService service;
    
    private List<Double> samplePnL;
    private static final String TEST_USER = "testUser";
    private static final double TEST_CONFIDENCE = 0.95;
    
    @BeforeEach
    void setUp() {
        samplePnL = List.of(-10.0, -5.0, 0.0, 5.0, 10.0, 15.0, 20.0);
    }
    
    @Test
    @DisplayName("Should calculate trade VaR successfully")
    void shouldCalculateTradeVarSuccessfully() {
        // Given
        String tradeId = "TRADE-001";
        double expectedVar = 8.5;
        TradeVarRequest request = new TradeVarRequest(tradeId, samplePnL, TEST_CONFIDENCE);
        
        when(strategy.calculateTradeVaR(samplePnL, TEST_CONFIDENCE)).thenReturn(expectedVar);
        
        // When
        VarResponse response = service.calculateTradeVaR(request, TEST_USER);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(tradeId);
        assertThat(response.var()).isEqualTo(expectedVar);
        assertThat(response.confidenceLevel()).isEqualTo(TEST_CONFIDENCE);
        assertThat(response.calculationMethod()).isEqualTo("HISTORICAL_SIMULATION");
        assertThat(response.tradeCount()).isEqualTo(1);
        
        verify(strategy).calculateTradeVaR(samplePnL, TEST_CONFIDENCE);
        verify(auditService).logRequest(eq(TEST_USER), eq("/api/v1/var/trade"), anyLong(), eq(true), isNull());
    }
    
    @Test
    @DisplayName("Should handle trade VaR calculation failure")
    void shouldHandleTradeVarCalculationFailure() {
        // Given
        String tradeId = "TRADE-002";
        TradeVarRequest request = new TradeVarRequest(tradeId, samplePnL, TEST_CONFIDENCE);
        String errorMessage = "Insufficient data";
        
        when(strategy.calculateTradeVaR(samplePnL, TEST_CONFIDENCE))
            .thenThrow(new IllegalArgumentException(errorMessage));
        
        // When/Then
        assertThatThrownBy(() -> service.calculateTradeVaR(request, TEST_USER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(errorMessage);
        
        verify(auditService).logRequest(eq(TEST_USER), eq("/api/v1/var/trade"), anyLong(), eq(false), eq(errorMessage));
    }
    
    @Test
    @DisplayName("Should calculate portfolio VaR successfully")
    void shouldCalculatePortfolioVarSuccessfully() {
        // Given
        String portfolioId = "PORT-001";
        double expectedVar = 12.3;
        Trade trade1 = new Trade("T1", samplePnL);
        Trade trade2 = new Trade("T2", samplePnL);
        PortfolioVarRequest request = new PortfolioVarRequest(portfolioId, TEST_CONFIDENCE, List.of(trade1, trade2));
        
        when(strategy.calculatePortfolioVaR(ArgumentMatchers.<List<List<Double>>>any(), eq(TEST_CONFIDENCE))).thenReturn(expectedVar);
        
        // When
        VarResponse response = service.calculatePortfolioVaR(request, TEST_USER);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(portfolioId);
        assertThat(response.var()).isEqualTo(expectedVar);
        assertThat(response.tradeCount()).isEqualTo(2);
        
        verify(strategy).calculatePortfolioVaR(ArgumentMatchers.<List<List<Double>>>any(), eq(TEST_CONFIDENCE));
        verify(auditService).logRequest(eq(TEST_USER), eq("/api/v1/var/portfolio"), anyLong(), eq(true), isNull());
    }
    
    @Test
    @DisplayName("Should handle portfolio VaR calculation failure")
    void shouldHandlePortfolioVarCalculationFailure() {
        // Given
        String portfolioId = "PORT-002";
        Trade trade = new Trade("T1", samplePnL);
        PortfolioVarRequest request = new PortfolioVarRequest(portfolioId, TEST_CONFIDENCE, List.of(trade));
        String errorMessage = "Invalid portfolio data";
        
        when(strategy.calculatePortfolioVaR(ArgumentMatchers.<List<List<Double>>>any(), eq(TEST_CONFIDENCE)))
            .thenThrow(new IllegalArgumentException(errorMessage));
        
        // When/Then
        assertThatThrownBy(() -> service.calculatePortfolioVaR(request, TEST_USER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(errorMessage);
        
        verify(auditService).logRequest(eq(TEST_USER), eq("/api/v1/var/portfolio"), anyLong(), eq(false), eq(errorMessage));
    }
    
    @Test
    @DisplayName("Should handle empty trade list in portfolio")
    void shouldHandleEmptyTradeList() {
        // Given
        String portfolioId = "PORT-003";
        PortfolioVarRequest request = new PortfolioVarRequest(portfolioId, TEST_CONFIDENCE, List.of());
        
        when(strategy.calculatePortfolioVaR(ArgumentMatchers.<List<List<Double>>>any(), eq(TEST_CONFIDENCE)))
            .thenThrow(new IllegalArgumentException("No trades provided"));
        
        // When/Then
        assertThatThrownBy(() -> service.calculatePortfolioVaR(request, TEST_USER))
            .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    @DisplayName("Should handle null username gracefully")
    void shouldHandleNullUsername() {
        // Given
        TradeVarRequest request = new TradeVarRequest("TRADE-003", samplePnL, TEST_CONFIDENCE);
        when(strategy.calculateTradeVaR(samplePnL, TEST_CONFIDENCE)).thenReturn(5.0);
        
        // When
        VarResponse response = service.calculateTradeVaR(request, null);
        
        // Then
        assertThat(response).isNotNull();
        verify(auditService).logRequest(isNull(), anyString(), anyLong(), eq(true), isNull());
    }
    
    @Test
    @DisplayName("Should calculate VaR with different confidence levels")
    void shouldCalculateVarWithDifferentConfidenceLevels() {
        // Given
        String tradeId = "TRADE-004";
        double confidence99 = 0.99;
        double expectedVar = 15.0;
        TradeVarRequest request = new TradeVarRequest(tradeId, samplePnL, confidence99);
        
        when(strategy.calculateTradeVaR(samplePnL, confidence99)).thenReturn(expectedVar);
        
        // When
        VarResponse response = service.calculateTradeVaR(request, TEST_USER);
        
        // Then
        assertThat(response.confidenceLevel()).isEqualTo(confidence99);
        assertThat(response.var()).isEqualTo(expectedVar);
    }
}
