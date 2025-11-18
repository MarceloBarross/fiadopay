package edu.ucsal.fiadopay.annotations;

import java.math.BigDecimal;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AntiFraudValidator implements ConstraintValidator<AntiFraud, BigDecimal>{

    private BigDecimal threshold;
    
    @Override
    public void initialize(AntiFraud constraintAnnotation) {
        this.threshold = BigDecimal.valueOf(constraintAnnotation.threshold());
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        return value.compareTo(threshold) <= 0;
    }

}
