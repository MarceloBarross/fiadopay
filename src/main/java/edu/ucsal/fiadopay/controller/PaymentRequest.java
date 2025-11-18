package edu.ucsal.fiadopay.controller;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

import edu.ucsal.fiadopay.annotations.AntiFraud;
import edu.ucsal.fiadopay.annotations.RequeridoSeValorIgual;

@RequeridoSeValorIgual(
    campoGatilho = "method",
    valorQueDisparaObrigatoriedade = "CARD",
    campoQueSeTornaObrigatorio = "installments",
    message = "O número de parcelas (installments) é obrigatório para pagamentos do tipo CARD."
)
public record PaymentRequest(
    @NotBlank @Pattern(regexp = "(?i)CARD|PIX|DEBIT|BOLETO") String method,
    @NotBlank String currency,
    @NotNull @AntiFraud(threshold = 2000000) @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @Min(1) @Max(12) Integer installments,
    @Size(max = 255) String metadataOrderId
) {}
