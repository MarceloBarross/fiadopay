package edu.ucsal.fiadopay.annotations;

import java.math.BigDecimal;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AntiFraudValidator implements ConstraintValidator<AntiFraud, BigDecimal>{

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        return true;
    }

}
