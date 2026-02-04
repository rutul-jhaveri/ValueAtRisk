package com.var.calculation.strategy;

import com.var.calculation.config.VarCalculationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for HistoricalSimulationStrategy.
 * Tests VaR calculation algorithm with various scenarios.
 */
@DisplayName("HistoricalSimulationStrategy Tests")
class HistoricalSimulationStrategyTest {
    
    private HistoricalSimulationStrategy strategy;
    
    @BeforeEach
    void setUp() {
        VarCalculationProperties properties = new VarCalculationProperties();
        properties.setMinDataPoints(5);
        strategy = new HistoricalSimulationStrategy(properties);
    }
    
    @Test
    @DisplayName("Should calculate VaR correctly for simple dataset")
    void shouldCalculateVarForSimpleDataset() {
        // Given
        List<Double> pnl = List.of(-10.0, -5.0, 0.0, 5.0, 10.0, 15.0, 20.0);
        double confidence = 0.95;
        
        // When
        double var = strategy.calculateTradeVaR(pnl, confidence);
        
        // Then
        assertThat(var).isGreaterThan(0);
        assertThat(var).isLessThanOrEqualTo(10.0);
    }
    
    @Test
    @DisplayName("Should throw exception for insufficient data")
    void shouldThrowExceptionForInsufficientData() {
        // Given
        List<Double> pnl = List.of(1.0, 2.0, 3.0);  // Less than 5 points
        
        // When/Then
        assertThatThrownBy(() -> strategy.calculateTradeVaR(pnl, 0.95))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Need at least 5 data points");
    }
    
    @Test
    @DisplayName("Should throw exception for null P&L")
    void shouldThrowExceptionForNullPnL() {
        // When/Then
        assertThatThrownBy(() -> strategy.calculateTradeVaR(null, 0.95))
            .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    @DisplayName("Should throw exception for invalid confidence level")
    void shouldThrowExceptionForInvalidConfidence() {
        // Given
        List<Double> pnl = generatePnL(50);
        
        // When/Then
        assertThatThrownBy(() -> strategy.calculateTradeVaR(pnl, 1.5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Confidence level must be between 0 and 1");
    }
    
    @ParameterizedTest
    @ValueSource(doubles = {0.90, 0.95, 0.99})
    @DisplayName("Should calculate VaR for different confidence levels")
    void shouldCalculateVarForDifferentConfidenceLevels(double confidence) {
        // Given
        List<Double> pnl = generatePnL(100);
        
        // When
        double var = strategy.calculateTradeVaR(pnl, confidence);
        
        // Then
        assertThat(var).isGreaterThan(0);
    }
    
    @Test
    @DisplayName("Should calculate higher VaR for higher confidence level")
    void shouldCalculateHigherVarForHigherConfidence() {
        // Given
        List<Double> pnl = generatePnL(100);
        
        // When
        double var95 = strategy.calculateTradeVaR(pnl, 0.95);
        double var99 = strategy.calculateTradeVaR(pnl, 0.99);
        
        // Then
        assertThat(var99).isGreaterThan(var95);
    }
    
    @Test
    @DisplayName("Should calculate portfolio VaR with diversification effect")
    void shouldCalculatePortfolioVarWithDiversification() {
        // Given
        List<Double> trade1 = generatePnL(50);
        List<Double> trade2 = generateNegativeCorrelatedPnL(50);
        List<List<Double>> portfolio = List.of(trade1, trade2);
        
        // When
        double portfolioVar = strategy.calculatePortfolioVaR(portfolio, 0.95);
        double trade1Var = strategy.calculateTradeVaR(trade1, 0.95);
        double trade2Var = strategy.calculateTradeVaR(trade2, 0.95);
        
        // Then - Portfolio VaR should be less than sum due to diversification
        assertThat(portfolioVar).isLessThan(trade1Var + trade2Var);
    }
    
    @Test
    @DisplayName("Should handle all negative P&L")
    void shouldHandleAllNegativePnL() {
        // Given
        List<Double> pnl = List.of(-50.0, -40.0, -30.0, -20.0, -10.0, -5.0, -3.0, -2.0, -1.0, -0.5,
                                   -50.0, -40.0, -30.0, -20.0, -10.0, -5.0, -3.0, -2.0, -1.0, -0.5,
                                   -50.0, -40.0, -30.0, -20.0, -10.0, -5.0, -3.0, -2.0, -1.0, -0.5);
        
        // When
        double var = strategy.calculateTradeVaR(pnl, 0.95);
        
        // Then
        assertThat(var).isGreaterThan(0);
    }
    
    @Test
    @DisplayName("Should handle all positive P&L")
    void shouldHandleAllPositivePnL() {
        // Given
        List<Double> pnl = List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0,
                                   1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0,
                                   1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);
        
        // When
        double var = strategy.calculateTradeVaR(pnl, 0.95);
        
        // Then
        assertThat(var).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    @DisplayName("Should throw exception for empty portfolio")
    void shouldThrowExceptionForEmptyPortfolio() {
        // Given
        List<List<Double>> emptyPortfolio = List.of();
        
        // When/Then
        assertThatThrownBy(() -> strategy.calculatePortfolioVaR(emptyPortfolio, 0.95))
            .isInstanceOf(IllegalArgumentException.class);
    }
    
    // Helper methods
    private List<Double> generatePnL(int size) {
        return java.util.stream.IntStream.range(0, size)
            .mapToDouble(i -> (i - size/2.0) * 0.5)
            .boxed()
            .toList();
    }
    
    private List<Double> generateNegativeCorrelatedPnL(int size) {
        return java.util.stream.IntStream.range(0, size)
            .mapToDouble(i -> (size/2.0 - i) * 0.5)
            .boxed()
            .toList();
    }
}
