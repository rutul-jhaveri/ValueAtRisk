package com.var.calculation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for VaR calculation.
 */
@Component
@ConfigurationProperties(prefix = "var.calculation")
public class VarCalculationProperties {
    
    private int minDataPoints = 5;

    public int getMinDataPoints() {
        return minDataPoints;
    }

    public void setMinDataPoints(int minDataPoints) {
        this.minDataPoints = minDataPoints;
    }
}
