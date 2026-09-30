package com.suraksha.ai.tools;

import com.suraksha.ai.model.ClaimRecord;
import com.suraksha.ai.model.PolicyRecord;
import com.suraksha.ai.repository.ClaimRecordRepository;
import com.suraksha.ai.repository.PolicyRecordRepository;
import com.suraksha.ai.security.PromptGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Read-only tools the support chatbot can call (Spring AI tool calling).
 *
 * Security model — the important part:
 *  - The customer's id comes from the validated JWT, via {@link ToolContext}. It is never a
 *    tool parameter, so the model can't be talked into looking at someone else's data.
 *  - Results deliberately exclude fraud score, risk level and the AI triage note/recommendation:
 *    those are internal to adjusters and must never reach a customer.
 *  - Claim descriptions are customer-written text, so they're wrapped as untrusted content.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupportTools {

    public static final String USER_ID_KEY = "userId";
    private static final int MAX_CLAIMS = 10;
    private static final int MAX_DESCRIPTION_CHARS = 300;

    private final PolicyRecordRepository policyRepository;
    private final ClaimRecordRepository claimRepository;

    public record PolicyInfo(String policyNumber, String planName, String type,
                             BigDecimal coverageAmount, BigDecimal premium,
                             LocalDate startDate, LocalDate endDate) {}

    public record ClaimInfo(String claimId, String policyNumber, String claimType,
                            BigDecimal claimAmount, LocalDate incidentDate,
                            String recordedStatus, String description) {}

    @Tool(description = "List the signed-in customer's own insurance policies: policy number, plan name, "
            + "type (HEALTH, MOTOR or LIFE), coverage amount, premium and start/end dates. "
            + "Use this before answering questions about what the customer is covered for.")
    public List<PolicyInfo> getMyPolicies(ToolContext toolContext) {
        UUID userId = currentUser(toolContext);
        if (userId == null) return List.of();
        log.info("tool getMyPolicies invoked");
        return policyRepository.findByUserId(userId).stream()
                .map(p -> new PolicyInfo(p.getPolicyNumber(), p.getPlanName(),
                        p.getType() == null ? null : p.getType().name(),
                        p.getCoverageAmount(), p.getPremium(), p.getStartDate(), p.getEndDate()))
                .toList();
    }

    @Tool(description = "List the signed-in customer's own claims (most recent first, at most 10) with the status "
            + "currently recorded for each: SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED or SETTLED. "
            + "Report the recorded status as-is; never predict or comment on a claim's outcome.")
    public List<ClaimInfo> getMyClaims(
            @ToolParam(required = false,
                    description = "Optional policy number (for example POL-HL-2201394) to only list that policy's claims. "
                            + "Leave empty for claims across all of the customer's policies.")
            String policyNumber,
            ToolContext toolContext) {
        UUID userId = currentUser(toolContext);
        if (userId == null) return List.of();
        log.info("tool getMyClaims invoked");

        List<PolicyRecord> policies = policyRepository.findByUserId(userId).stream()
                .filter(p -> policyNumber == null || policyNumber.isBlank()
                        || policyNumber.trim().equalsIgnoreCase(p.getPolicyNumber()))
                .toList();

        return policies.stream()
                .flatMap(p -> claimRepository.findByPolicyIdOrderByIncidentDateDesc(p.getId()).stream()
                        .map(c -> toInfo(c, p)))
                .sorted(Comparator.comparing(ClaimInfo::incidentDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_CLAIMS)
                .toList();
    }

    private static ClaimInfo toInfo(ClaimRecord c, PolicyRecord p) {
        String description = c.getDescription() == null ? "" : c.getDescription();
        if (description.length() > MAX_DESCRIPTION_CHARS) {
            description = description.substring(0, MAX_DESCRIPTION_CHARS) + "…";
        }
        return new ClaimInfo(
                c.getId() == null ? null : c.getId().toString(),
                p.getPolicyNumber(), c.getClaimType(), c.getClaimAmount(), c.getIncidentDate(),
                c.getStatus() == null ? null : c.getStatus().name(),
                PromptGuard.wrapUntrusted(description));
    }

    private static UUID currentUser(ToolContext toolContext) {
        Object raw = toolContext == null ? null : toolContext.getContext().get(USER_ID_KEY);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
