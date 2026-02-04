package com.var.calculation.validation;

import com.var.calculation.config.VarCalculationProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Validator for minimum data points requirement.
 */
@Component
public class MinDataPointsValidator implements ConstraintValidator<MinDataPoints, List<Double>> {

    private final VarCalculationProperties properties;

    public MinDataPointsValidator(VarCalculationProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(List<Double> value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // @NotNull handles null check
        }
        
        int minPoints = properties.getMinDataPoints();
        boolean valid = value.size() >= minPoints;
        
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("At least %d historical data points required", minPoints)
            ).addConstraintViolation();
        }
        
        return valid;
    }
}
