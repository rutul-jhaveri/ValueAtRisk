package com.var.calculation.strategy;

import com.var.calculation.config.VarCalculationProperties;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class HistoricalSimulationStrategy {
    
    private final VarCalculationProperties properties;
    
    public HistoricalSimulationStrategy(VarCalculationProperties properties) {
        this.properties = properties;
    }
    
    public double calculateTradeVaR(List<Double> historicalPnL, double confidenceLevel) {
        validateInput(historicalPnL, confidenceLevel);
        
        var sorted = historicalPnL.stream().sorted().toList();
        double percentile = 1 - confidenceLevel;
        double position = percentile * (sorted.size() - 1);
        
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        
        double var = (lower == upper) 
            ? sorted.get(lower)
            : interpolate(sorted.get(lower), sorted.get(upper), position - lower);
        
        return Math.abs(var);
    }
    
    public double calculatePortfolioVaR(List<List<Double>> tradesPnL, double confidenceLevel) {
        if (tradesPnL == null || tradesPnL.isEmpty()) {
            throw new IllegalArgumentException("Portfolio must contain at least one trade");
        }
        
        int periods = tradesPnL.get(0).size();
        int numTrades = tradesPnL.size();
        
        // Check all trades have same number of periods
        for (List<Double> trade : tradesPnL) {
            if (trade.size() != periods) {
                throw new IllegalArgumentException("All trades must have the same number of data points");
            }
        }
        
        // Aggregate P&L across trades for each period
        var portfolioPnL = new ArrayList<Double>(periods);
        for (int i = 0; i < periods; i++) {
            double sum = 0;
            for (int j = 0; j < numTrades; j++) {
                sum += tradesPnL.get(j).get(i);
            }
            portfolioPnL.add(sum);
        }
        
        return calculateTradeVaR(portfolioPnL, confidenceLevel);
    }
    
    private double interpolate(double lower, double upper, double fraction) {
        return lower + fraction * (upper - lower);
    }
    
    private void validateInput(List<Double> data, double confidenceLevel) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Historical data is required");
        }
        
        int minPoints = properties.getMinDataPoints();
        if (data.size() < minPoints) {
            throw new IllegalArgumentException(
                "Need at least " + minPoints + " data points for reliable VaR calculation");
        }
        
        if (confidenceLevel <= 0 || confidenceLevel >= 1) {
            throw new IllegalArgumentException("Confidence level must be between 0 and 1");
        }
    }
}
