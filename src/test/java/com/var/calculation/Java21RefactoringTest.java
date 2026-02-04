package com.var.calculation;

import com.var.calculation.model.dto.*;
import com.var.calculation.strategy.HistoricalSimulationStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Java 21 refactored VaR calculation service.
 * Tests records, virtual threads, and enhanced algorithms.
 */
@SpringBootTest
class Java21RefactoringTest {
    
    @Autowired
    private HistoricalSimulationStrategy strategy;
    
    /**
     * Test that records work correctly with validation.
     */
    @Test
    void testRecordCreation() {
        var historicalPnL = List.of(-10.0, -5.0, 0.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 35.0,
                                    40.0, 45.0, 50.0, 55.0, 60.0, 65.0, 70.0, 75.0, 80.0, 85.0,
                                    90.0, 95.0, 100.0, 105.0, 110.0, 115.0, 120.0, 125.0, 130.0, 135.0);
        
        var request = new TradeVarRequest("TRADE-001", historicalPnL, 0.95);
        
        assertEquals("TRADE-001", request.tradeId());
        assertEquals(30, request.historicalPnL().size());
        assertEquals(0.95, request.confidenceLevel());
    }
    
    /**
     * Test record equality (automatic implementation).
     */
    @Test
    void testRecordEquality() {
        var historicalPnL = List.of(-10.0, -5.0, 0.0, 5.0, 10.0, 15.0, 20.0, 25.0, 30.0, 35.0,
                                    40.0, 45.0, 50.0, 55.0, 60.0, 65.0, 70.0, 75.0, 80.0, 85.0,
                                    90.0, 95.0, 100.0, 105.0, 110.0, 115.0, 120.0, 125.0, 130.0, 135.0);
        
        var request1 = new TradeVarRequest("TRADE-001", historicalPnL, 0.95);
        var request2 = new TradeVarRequest("TRADE-001", historicalPnL, 0.95);
        
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }
    
    /**
     * Test VaR calculation with refactored algorithm.
     */
    @Test
    void testVarCalculation() {
        var historicalPnL = List.of(-10.0, -8.0, -5.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0,
                                    4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0,
                                    14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0);
        
        double var = strategy.calculateTradeVaR(historicalPnL, 0.95);
        
        assertTrue(var >= 0, "VaR should be non-negative");
        assertTrue(var <= 10.0, "VaR should be reasonable for this dataset");
    }
    
    /**
     * Test portfolio VaR with diversification effect.
     */
    @Test
    void testPortfolioVarDiversification() {
        // Trade 1: Positive trend
        var trade1PnL = List.of(-10.0, -8.0, -5.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0,
                               4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0,
                               14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0);
        
        // Trade 2: Negative correlation (opposite trend)
        var trade2PnL = List.of(10.0, 8.0, 5.0, 3.0, 2.0, 1.0, 0.0, -1.0, -2.0, -3.0,
                               -4.0, -5.0, -6.0, -7.0, -8.0, -9.0, -10.0, -11.0, -12.0, -13.0,
                               -14.0, -15.0, -16.0, -17.0, -18.0, -19.0, -20.0, -21.0, -22.0, -23.0);
        
        double var1 = strategy.calculateTradeVaR(trade1PnL, 0.95);
        double var2 = strategy.calculateTradeVaR(trade2PnL, 0.95);
        double portfolioVar = strategy.calculatePortfolioVaR(List.of(trade1PnL, trade2PnL), 0.95);
        
        // Portfolio VaR should be less than sum due to diversification
        assertTrue(portfolioVar < (var1 + var2), 
                  "Portfolio VaR should show diversification benefit");
    }
    
    /**
     * Test sequenced collections (getFirst).
     */
    @Test
    void testSequencedCollections() {
        var trades = List.of(
            List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0),
            List.of(4.0, 5.0, 6.0, 7.0, 8.0, 9.0)
        );
        
        // This uses getFirst() internally in the refactored code
        assertDoesNotThrow(() -> strategy.calculatePortfolioVaR(trades, 0.95));
    }
    
    /**
     * Test enhanced stream processing.
     */
    @Test
    void testStreamProcessing() {
        var trades = IntStream.range(0, 5)
            .mapToObj(i -> IntStream.range(0, 30)
                .mapToDouble(j -> Math.random() * 100 - 50)
                .boxed()
                .toList())
            .toList();
        
        assertDoesNotThrow(() -> strategy.calculatePortfolioVaR(trades, 0.95));
    }
    
    /**
     * Test validation error handling.
     */
    @Test
    void testInsufficientData() {
        var insufficientData = List.of(1.0);
        
        assertThrows(IllegalArgumentException.class, 
                    () -> strategy.calculateTradeVaR(insufficientData, 0.95),
                    "Should throw exception for insufficient data");
    }
    
    /**
     * Test edge case: all same values.
     */
    @Test
    void testUniformData() {
        var uniformData = IntStream.range(0, 30)
            .mapToDouble(i -> 10.0)
            .boxed()
            .toList();
        
        double var = strategy.calculateTradeVaR(uniformData, 0.95);
        assertEquals(10.0, var, 0.001, "VaR should be 10 for uniform data of value 10");
    }
    
    /**
     * Test different confidence levels.
     */
    @Test
    void testConfidenceLevels() {
        var historicalPnL = IntStream.range(0, 100)
            .mapToDouble(i -> i - 50.0)
            .boxed()
            .toList();
        
        double var95 = strategy.calculateTradeVaR(historicalPnL, 0.95);
        double var99 = strategy.calculateTradeVaR(historicalPnL, 0.99);
        
        assertTrue(var99 >= var95, "Higher confidence should yield higher VaR");
    }
}
