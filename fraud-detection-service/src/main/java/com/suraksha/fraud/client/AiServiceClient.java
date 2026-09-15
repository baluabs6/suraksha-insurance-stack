package com.suraksha.fraud.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suraksha.fraud.events.ClaimFlaggedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Originally published to Kafka's "claim.flagged" topic and consumed by
// ai-service. Rewritten as a direct internal HTTP call — see the note in
// backend's ClaimEventPublisher for why. Fire-and-forget for the same
// reason: a flagged claim is already saved with its risk score by the time
// this fires, so a slow/unreachable ai-service should never fail the
// caller's request — it just means the AI triage note doesn't get written
// yet, which is recoverable (a backfill job could re-check unflagged claims
// with no aiSummary).
@Component
@Slf4j
public class AiServiceClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Value("${ai.service.base-url}")
    private String aiServiceBaseUrl;

    // Must match ai-service's app.internal-service-token — authenticates this
    // service-to-service call now that /internal/** requires it.
    @Value("${app.internal-service-token}")
    private String internalServiceToken;

    // Same normalization as backend's ClaimEventPublisher — Render's private
    // fromService/hostport value has no scheme.
    private String resolvedBaseUrl() {
        return aiServiceBaseUrl.contains("://") ? aiServiceBaseUrl : "http://" + aiServiceBaseUrl;
    }

    public void notifyClaimFlagged(ClaimFlaggedEvent event) {
        try {
            String body = mapper.writeValueAsString(event);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolvedBaseUrl() + "/internal/claims/flagged"))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Service-Token", internalServiceToken)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((response, ex) -> {
                        if (ex != null) {
                            log.error("Failed to notify ai-service for claim {}", event.claimId(), ex);
                        } else if (response.statusCode() >= 300) {
                            log.error("ai-service returned {} for claim {}", response.statusCode(), event.claimId());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to build claim.flagged payload for claim {}", event.claimId(), e);
        }
    }
}
