package edu.ucsal.fiadopay.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = AntiFraudValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AntiFraud {

    double threshold() default 1000000;

    String message() default "Suspeita de fraude";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
