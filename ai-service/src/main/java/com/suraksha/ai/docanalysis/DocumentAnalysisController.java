package com.suraksha.ai.docanalysis;

import com.suraksha.ai.client.AnthropicClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Analyzes a photo attached to a claim (damage photo, medical bill, repair
 * estimate, etc.) and writes a short factual description an adjuster can
 * skim instead of opening the image themselves. Deliberately descriptive
 * only — it never estimates a payout or says whether the claim looks
 * fraudulent; that stays fraud-detection-service's job, which has the actual
 * claim/policy data to reason over. This endpoint only sees the one image
 * it's given.
 */
@RestController
@RequestMapping("/api/ai/documents")
@RequiredArgsConstructor
public class DocumentAnalysisController {

    private static final String SYSTEM_PROMPT = """
            You are looking at a single photo attached to an insurance claim
            (could be vehicle damage, a medical bill, a repair estimate, a
            property photo, or similar). Write a short, factual description of
            what's visible in the image — 2-3 sentences.

            Rules:
            - Describe only what you can actually see. Don't guess at costs,
              causes, dates, or anything not visible in the image.
            - If the claimant's description is provided, note in one clause
              whether the photo is consistent with it or not — do not
              speculate about intent either way, just state the observation.
            - Never state or imply whether the claim should be approved,
              denied, or is fraudulent. That is a human adjuster's decision.
            - If the image is unclear, low quality, or doesn't look like
              claim-relevant documentation, say so plainly instead of guessing.
            """;

    private final AnthropicClient anthropicClient;

    @PostMapping("/analyze")
    public DocumentAnalysisResponse analyze(@Valid @RequestBody DocumentAnalysisRequest req) {
        String claimType = req.getClaimType() == null || req.getClaimType().isBlank()
                ? "unspecified" : req.getClaimType();
        String claimantDescription = req.getClaimantDescription() == null || req.getClaimantDescription().isBlank()
                ? "(none provided)" : req.getClaimantDescription();

        String userMessage = """
                Claim type: %s
                Claimant's own description of what happened: %s

                Describe what's visible in the attached image.
                """.formatted(claimType, claimantDescription);

        String fallback = "AI document analysis unavailable (ANTHROPIC_API_KEY not configured on ai-service). "
                + "Please review the attached image manually.";

        String summary = anthropicClient.completeWithImage(
                SYSTEM_PROMPT, userMessage, req.getImageBase64(), req.getMediaType(), fallback);

        return new DocumentAnalysisResponse(summary.trim(), summary.equals(fallback));
    }
}
