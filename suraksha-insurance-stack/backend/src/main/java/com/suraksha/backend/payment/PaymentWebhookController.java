package com.suraksha.backend.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suraksha.backend.payment.gateway.WebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Receives Razorpay's server-to-server webhook — this, not the checkout
 * modal's client-side success callback, is the only source of truth for
 * whether money actually moved. Public endpoint by necessity (Razorpay's
 * servers can't hold a session cookie or CSRF token), so it's excluded from
 * both auth and CSRF in SecurityConfig — the HMAC signature is what protects
 * it instead. Configure this URL in the Razorpay dashboard as the webhook
 * target for the "payment.captured" and "payment.failed" events.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final WebhookSignatureVerifier signatureVerifier;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/api/payments/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody String rawBody,
                                            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        if (!signatureVerifier.isValid(rawBody, signature)) {
            log.warn("Rejected webhook call with invalid or missing signature.");
            return ResponseEntity.status(400).body("invalid signature");
        }

        try {
            JsonNode root = mapper.readTree(rawBody);
            String eventType = root.path("event").asText();
            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
            String orderId = paymentEntity.path("order_id").asText(null);
            String paymentId = paymentEntity.path("id").asText(null);

            if (orderId == null) {
                return ResponseEntity.ok("ignored: no order_id in payload");
            }

            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (payment == null) {
                log.warn("Webhook for unknown order_id {}", orderId);
                return ResponseEntity.ok("ignored: unknown order");
            }

            switch (eventType) {
                case "payment.captured" -> {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setGatewayTxnId(paymentId);
                    payment.setPaidAt(Instant.now());
                    paymentRepository.save(payment);
                    log.info("Payment {} confirmed PAID via webhook (order {})", payment.getId(), orderId);
                }
                case "payment.failed" -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setGatewayTxnId(paymentId);
                    paymentRepository.save(payment);
                    log.info("Payment {} marked FAILED via webhook (order {})", payment.getId(), orderId);
                }
                default -> log.info("Unhandled webhook event type: {}", eventType);
            }

            return ResponseEntity.ok("processed");
        } catch (Exception e) {
            log.error("Failed to process Razorpay webhook.", e);
            return ResponseEntity.status(500).body("processing error");
        }
    }
}
