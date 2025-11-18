package edu.ucsal.fiadopay.annotations;
import java.lang.reflect.Field;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RequeridoSeValorIgualValidator implements ConstraintValidator<RequeridoSeValorIgual, Object>{

    private String campoGatilho;
    private String valorEsperado;
    private String campoAlvo;

    @Override
    public void initialize(RequeridoSeValorIgual constraint) {
        this.campoGatilho = constraint.campoGatilho();
        this.valorEsperado = constraint.valorQueDisparaObrigatoriedade();
        this.campoAlvo = constraint.campoQueSeTornaObrigatorio();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) return true;
        
        try {
            Field fieldGatilho = obj.getClass().getDeclaredField(campoGatilho);
            fieldGatilho.setAccessible(true);
            Object valorAtualGatilho = fieldGatilho.get(obj);

            if (valorAtualGatilho.toString().equalsIgnoreCase(valorEsperado)) {
                Field fieldAlvo = obj.getClass().getDeclaredField(campoAlvo);
                fieldAlvo.setAccessible(true);
                Object valorAlvo = fieldAlvo.get(obj);

                if (valorAlvo == null) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            System.err.println("Erro de Reflection: Verifique os nomes dos campos na anotação. " + e.getMessage());
            return false;
        }
    }

}
