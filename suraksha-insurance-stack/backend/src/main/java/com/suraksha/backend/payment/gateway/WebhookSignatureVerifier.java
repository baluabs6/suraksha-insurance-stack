package com.suraksha.backend.payment.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifies the X-Razorpay-Signature header: HMAC-SHA256 of the raw request
 * body, keyed with the webhook secret, hex-encoded. This is why the webhook
 * controller reads the body as a raw string instead of letting Spring bind
 * it to a DTO first — signature verification needs the exact bytes Razorpay
 * signed, before any JSON parsing/re-serialization could change them.
 */
@Component
public class WebhookSignatureVerifier {

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public boolean isConfigured() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    public boolean isValid(String rawBody, String signatureHeader) {
        if (!isConfigured() || signatureHeader == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            // Constant-time comparison — a timing side-channel on signature
            // comparison is a real (if narrow) attack surface on webhooks.
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}
