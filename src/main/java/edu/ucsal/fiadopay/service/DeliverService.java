package edu.ucsal.fiadopay.service;

import edu.ucsal.fiadopay.repo.WebhookDeliveryRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

@Service
public class DeliverService {

    private final WebhookDeliveryRepository deliveries;

    public DeliverService(WebhookDeliveryRepository deliveries) {
        this.deliveries = deliveries;
    }

     void tryDeliver(Long deliveryId){
        var d = deliveries.findById(deliveryId).orElse(null);
        if (d==null) return;
        try {
            var client = HttpClient.newHttpClient();
            var req = HttpRequest.newBuilder(URI.create(d.getTargetUrl()))
                    .header("Content-Type","application/json")
                    .header("X-Event-Type", d.getEventType())
                    .header("X-Signature", d.getSignature())
                    .POST(HttpRequest.BodyPublishers.ofString(d.getPayload()))
                    .build();
            var res = client.send(req, HttpResponse.BodyHandlers.ofString());
            d.setAttempts(d.getAttempts()+1);
            d.setLastAttemptAt(Instant.now());
            d.setDelivered(res.statusCode()>=200 && res.statusCode()<300);
            deliveries.save(d);
            if(!d.isDelivered() && d.getAttempts()<5){
                Thread.sleep(1000L * d.getAttempts());
                tryDeliver(deliveryId);
            }
        } catch (Exception e){
            d.setAttempts(d.getAttempts()+1);
            d.setLastAttemptAt(Instant.now());
            d.setDelivered(false);
            deliveries.save(d);
            if (d.getAttempts()<5){
                try {
                    Thread.sleep(1000L * d.getAttempts());
                } catch (InterruptedException ignored) {}
                tryDeliver(deliveryId);
            }
        }
    }
}
