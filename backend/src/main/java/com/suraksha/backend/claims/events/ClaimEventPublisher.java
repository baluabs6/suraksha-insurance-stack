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

@Component
@Slf4j
public class ClaimEventPublisher {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Value("${fraud.service.base-url}")
    private String fraudServiceBaseUrl;

    @Value("${app.internal-service-token}")
    private String internalServiceToken;

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
                    .header("X-Internal-Service-Token", internalServiceToken)
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .whenComplete((response, ex) -> {
                        if (ex != null) {
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
