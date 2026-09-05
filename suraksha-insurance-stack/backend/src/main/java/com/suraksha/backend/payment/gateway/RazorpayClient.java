package com.suraksha.backend.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Thin wrapper around Razorpay's Orders API, following the same shape as
 * AnthropicClient: a plain HTTP call, and a graceful fallback when no real
 * credentials are configured, so the whole stack still runs and demos
 * end-to-end without anyone needing a merchant account on day one.
 */
@Component
@Slf4j
public class RazorpayClient {

    private static final String ORDERS_URL = "https://api.razorpay.com/v1/orders";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    public boolean isConfigured() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    public String getKeyId() {
        return keyId;
    }

    public record OrderResult(String orderId, boolean success) {
    }

    /**
     * Amount must be in paise (₹1 = 100 paise) — Razorpay's API works in the
     * smallest currency unit, same convention as Stripe's cents.
     */
    public OrderResult createOrder(BigDecimal amountInRupees, String receipt) {
        if (!isConfigured()) {
            return new OrderResult(null, false);
        }
        try {
            long amountInPaise = amountInRupees.multiply(BigDecimal.valueOf(100)).longValueExact();

            ObjectNode body = mapper.createObjectNode();
            body.put("amount", amountInPaise);
            body.put("currency", "INR");
            body.put("receipt", receipt);

            String basicAuth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ORDERS_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + basicAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Razorpay order creation failed ({}): {}", response.statusCode(), response.body());
                return new OrderResult(null, false);
            }

            JsonNode root = mapper.readTree(response.body());
            return new OrderResult(root.path("id").asText(), true);
        } catch (Exception e) {
            log.error("Razorpay order creation failed.", e);
            return new OrderResult(null, false);
        }
    }
}
