package edu.ucsal.fiadopay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsal.fiadopay.controller.PaymentRequest;
import edu.ucsal.fiadopay.controller.PaymentResponse;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.repo.MerchantRepository;
import edu.ucsal.fiadopay.repo.PaymentRepository;
import edu.ucsal.fiadopay.repo.WebhookDeliveryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentService {

  private final AutService autService;
  private final WebhookService webhookService;
  private final PaymentRepository payments;
  

  public PaymentService(AutService autService, WebhookService webhookService, PaymentRepository payments) {
      this.autService = autService;
      this.webhookService = webhookService;
      this.payments = payments;
  }

  @Transactional
  public PaymentResponse createPayment(String auth, String idemKey, PaymentRequest req){
    var merchant = autService.merchantFromAuth(auth);
    var mid = merchant.getId();

    if (idemKey != null) {
      var existing = payments.findByIdempotencyKeyAndMerchantId(idemKey, mid);
      if(existing.isPresent()) return toResponse(existing.get());
    }

    Double interest = null;
    BigDecimal total = req.amount();
    if ("CARD".equalsIgnoreCase(req.method()) && req.installments()!=null && req.installments()>1){
      interest = 1.0; // 1%/mês
      var base = new BigDecimal("1.01");
      var factor = base.pow(req.installments());
      total = req.amount().multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    var payment = Payment.builder()
        .id("pay_"+UUID.randomUUID().toString().substring(0,8))
        .merchantId(mid)
        .method(req.method().toUpperCase())
        .amount(req.amount())
        .currency(req.currency())
        .installments(req.installments()==null?1:req.installments())
        .monthlyInterest(interest)
        .totalWithInterest(total)
        .status(Payment.Status.PENDING)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .idempotencyKey(idemKey)
        .metadataOrderId(req.metadataOrderId())
        .build();

    payments.save(payment);

    CompletableFuture.runAsync(() -> webhookService.processAndWebhook(payment.getId()));

    return toResponse(payment);
  }

  public PaymentResponse getPayment(String id){
    return toResponse(payments.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  public Map<String,Object> refund(String auth, String paymentId){
    var merchant = autService.merchantFromAuth(auth);
    var p = payments.findById(paymentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!merchant.getId().equals(p.getMerchantId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    p.setStatus(Payment.Status.REFUNDED);
    p.setUpdatedAt(Instant.now());
    payments.save(p);
    webhookService.sendWebhook(p);
    return Map.of("id","ref_"+UUID.randomUUID(),"status","PENDING");
  }

  private PaymentResponse toResponse(Payment p){
    return new PaymentResponse(
        p.getId(), p.getStatus().name(), p.getMethod(),
        p.getAmount(), p.getInstallments(), p.getMonthlyInterest(),
        p.getTotalWithInterest()
    );
  }
}
