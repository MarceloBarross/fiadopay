package edu.ucsal.fiadopay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.domain.WebhookDelivery;
import edu.ucsal.fiadopay.repo.MerchantRepository;
import edu.ucsal.fiadopay.repo.PaymentRepository;
import edu.ucsal.fiadopay.repo.WebhookDeliveryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@Service
public class WebhookService {
    private final PaymentRepository payments;
    private final MerchantRepository merchants;
    private final WebhookDeliveryRepository deliveries;
    private final DeliverService deliverService;
    private final ObjectMapper objectMapper;

    @Value("${fiadopay.processing-delay-ms}") long delay;
    @Value("${fiadopay.failure-rate}") double failRate;
    @Value("${fiadopay.webhook-secret}") String secret;

    public WebhookService(PaymentRepository payments, MerchantRepository merchants, WebhookDeliveryRepository deliveries, DeliverService deliverService, ObjectMapper objectMapper) {
        this.payments = payments;
        this.merchants = merchants;
        this.deliveries = deliveries;
        this.deliverService = deliverService;
        this.objectMapper = objectMapper;
    }


    void processAndWebhook(String paymentId){
        try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
        var p = payments.findById(paymentId).orElse(null);
        if (p==null) return;

        var approved = Math.random() > failRate;
        p.setStatus(approved ? Payment.Status.APPROVED : Payment.Status.DECLINED);
        p.setUpdatedAt(Instant.now());
        payments.save(p);

        sendWebhook(p);
    }

    void sendWebhook(Payment p){
        var merchant = merchants.findById(p.getMerchantId()).orElse(null);
        if (merchant==null || merchant.getWebhookUrl()==null || merchant.getWebhookUrl().isBlank()) return;

        String payload;
        try {
            var data = Map.of(
                    "paymentId", p.getId(),
                    "status", p.getStatus().name(),
                    "occurredAt", Instant.now().toString()
            );
            var event = Map.of(
                    "id", "evt_"+ UUID.randomUUID().toString().substring(0,8),
                    "type", "payment.updated",
                    "data", data
            );
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            // fallback mínimo: não envia webhook se falhar a serialização
            return;
        }

        var signature = hmac(payload, secret);

        var delivery = deliveries.save(WebhookDelivery.builder()
                .eventId("evt_"+UUID.randomUUID().toString().substring(0,8))
                .eventType("payment.updated")
                .paymentId(p.getId())
                .targetUrl(merchant.getWebhookUrl())
                .signature(signature)
                .payload(payload)
                .attempts(0)
                .delivered(false)
                .lastAttemptAt(null)
                .build());

        CompletableFuture.runAsync(() -> deliverService.tryDeliver(delivery.getId()));
    }

    public boolean validarRecebimento(String signatureHeader, String payloadRaw) {
        if (signatureHeader == null || payloadRaw == null) {
            return false;
        }

        String signatureCalculada = hmac(payloadRaw, secret);

        return MessageDigest.isEqual(
                signatureHeader.getBytes(StandardCharsets.UTF_8),
                signatureCalculada.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String hmac(String payload, String secret){
        try {

            byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secretBytes, "HmacSHA256"));

            return Base64.getEncoder().encodeToString(mac.doFinal(payloadBytes));
        } catch (Exception e){ return ""; }
    }
}
