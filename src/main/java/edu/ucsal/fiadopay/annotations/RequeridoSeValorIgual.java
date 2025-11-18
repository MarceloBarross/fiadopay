package edu.ucsal.fiadopay.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE) 
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequeridoSeValorIgualValidator.class)
public @interface RequeridoSeValorIgual {
    String campoGatilho();
    String valorQueDisparaObrigatoriedade();
    String campoQueSeTornaObrigatorio();

    String message() default "O campo é obrigatório sob esta condição de pagamento.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
