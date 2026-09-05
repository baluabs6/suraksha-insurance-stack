package com.suraksha.backend.claims.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suraksha.backend.claims.Claim;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Originally published to Kafka's "claim.submitted" topic and consumed by
// fraud-detection-service. Rewritten as a direct internal HTTP call so this
// stack can run on platforms with no managed Kafka (e.g. Render's free
// tier). Kept fire-and-forget on purpose: the user's claim submission must
// never wait on — or fail because of — fraud-detection-service being slow,
// asleep (free-tier cold start), or down. If that matters in production,
// replace this with a durable outbox pattern instead of an HTTP call.
@Component
@Slf4j
public class ClaimEventPublisher {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Value("${fraud.service.base-url}")
    private String fraudServiceBaseUrl;

    // Render's private-network fromService/hostport value has no scheme
    // (e.g. "suraksha-fraud-detection:10000"); local/docker-compose values
    // already include one. Normalize so both work without extra config.
    private String resolvedBaseUrl() {
        return fraudServiceBaseUrl.contains("://") ? fraudServiceBaseUrl : "http://" + fraudServiceBaseUrl;
    }

    public void publishSubmitted(Claim claim) {
        ClaimSubmittedEvent event = new ClaimSubmittedEvent(
                claim.getId(),
                claim.getPolicy().getId(),
                claim.getUser().getId(),
                claim.getClaimType(),
                claim.getClaimAmount(),
                claim.getIncidentDate()
        );

        try {
            String body = mapper.writeValueAsString(event);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolvedBaseUrl() + "/internal/claims/submitted"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((response, ex) -> {
                        if (ex != null) {
                            // Don't fail the user's claim submission if fraud-detection-service
                            // is unreachable — log it; the claim still exists and can be scored
                            // later by a backfill job. Never let async scoring block the request.
                            log.error("Failed to notify fraud-detection-service for claim {}", claim.getId(), ex);
                        } else if (response.statusCode() >= 300) {
                            log.error("fraud-detection-service returned {} for claim {}",
                                    response.statusCode(), claim.getId());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to build claim.submitted payload for claim {}", claim.getId(), e);
        }
    }
}
