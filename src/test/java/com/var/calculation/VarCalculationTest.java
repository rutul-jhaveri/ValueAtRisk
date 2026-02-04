package com.var.calculation;

import com.var.calculation.config.VarCalculationProperties;
import com.var.calculation.strategy.HistoricalSimulationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VarCalculationTest {
    
    private HistoricalSimulationStrategy strategy;
    
    @BeforeEach
    void setUp() {
        VarCalculationProperties properties = new VarCalculationProperties();
        properties.setMinDataPoints(5);
        strategy = new HistoricalSimulationStrategy(properties);
    }
    
    @Test
    void testTradeVarCalculation() {
        List<Double> pnl = Arrays.asList(
            -10.0, -8.0, -5.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0,
            4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0,
            14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0,
            24.0, 25.0
        );
        
        double var = strategy.calculateTradeVaR(pnl, 0.95);
        
        assertTrue(var > 0, "VaR should be positive");
        assertTrue(var < 10, "VaR should be less than worst loss");
    }
    
    @Test
    void testInsufficientData() {
        List<Double> pnl = Arrays.asList(1.0, 2.0, 3.0);
        
        assertThrows(IllegalArgumentException.class, () -> {
            strategy.calculateTradeVaR(pnl, 0.95);
        });
    }
}
