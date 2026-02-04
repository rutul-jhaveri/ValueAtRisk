package com.var.calculation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that historical P&L data meets minimum data points requirement.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinDataPointsValidator.class)
@Documented
public @interface MinDataPoints {
    String message() default "Insufficient historical data points";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
