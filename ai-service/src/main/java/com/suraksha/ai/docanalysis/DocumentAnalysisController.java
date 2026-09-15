package com.suraksha.ai.docanalysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suraksha.ai.client.AnthropicClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
@Slf4j
public class DocumentAnalysisController {

    private static final String EXTRACTION_SYSTEM_PROMPT = """
            You are looking at a single photo of a document attached to an
            insurance claim (a medical bill, a repair estimate, an invoice, or
            similar). Extract the structured fields a claims adjuster would
            need to key into a system, so they don't have to read the image
            themselves.

            Respond with ONLY a single JSON object, no markdown fences, no
            preamble, matching exactly this shape:
            {
              "documentType": "<one short label, e.g. 'medical bill', 'repair estimate', 'invoice', 'unclear'>",
              "extractedAmount": "<the total amount shown, plain number no currency symbol or commas, or null if not visible>",
              "extractedDate": "<the document's date in YYYY-MM-DD if visible, or null>",
              "merchantOrProvider": "<the hospital/garage/vendor name shown, or null>",
              "lineItems": ["<short line item description>", ...],
              "notes": "<one short sentence flagging anything illegible, inconsistent, or missing — or empty string if nothing to flag>"
            }

            Rules:
            - Extract only what is actually visible in the image. Never
              invent an amount, date, or name that isn't shown.
            - lineItems should list at most 8 items; use an empty array if the
              document has no itemized breakdown.
            - Never state or imply whether the claim should be approved,
              denied, or looks fraudulent — that is a human adjuster's job.
            """;

    private final ObjectMapper mapper = new ObjectMapper();

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

    /**
     * Same input as /analyze, but returns structured fields instead of a
     * free-text summary — the natural next step once an adjuster has read
     * the description and wants the numbers keyed into the claim record
     * without retyping them by hand.
     */
    @PostMapping("/extract")
    public DocumentExtractionResponse extract(@Valid @RequestBody DocumentAnalysisRequest req) {
        String claimType = req.getClaimType() == null || req.getClaimType().isBlank()
                ? "unspecified" : req.getClaimType();

        String userMessage = "Claim type: " + claimType + "\n\nExtract the structured fields from the attached image.";

        String fallbackSentinel = "__FALLBACK__";
        String raw = anthropicClient.completeWithImage(
                EXTRACTION_SYSTEM_PROMPT, userMessage, req.getImageBase64(), req.getMediaType(), fallbackSentinel);

        if (fallbackSentinel.equals(raw) || !anthropicClient.isConfigured()) {
            return fallbackExtraction();
        }

        try {
            JsonNode node = mapper.readTree(stripCodeFences(raw));
            List<String> lineItems = new ArrayList<>();
            if (node.path("lineItems").isArray()) {
                node.path("lineItems").forEach(item -> lineItems.add(item.asText()));
            }
            return new DocumentExtractionResponse(
                    node.path("documentType").asText("unclear"),
                    nullableText(node, "extractedAmount"),
                    nullableText(node, "extractedDate"),
                    nullableText(node, "merchantOrProvider"),
                    lineItems,
                    node.path("notes").asText(""),
                    false
            );
        } catch (Exception e) {
            log.warn("Could not parse document-extraction JSON response, falling back.", e);
            return fallbackExtraction();
        }
    }

    private DocumentExtractionResponse fallbackExtraction() {
        return new DocumentExtractionResponse("unclear", null, null, null, List.of(),
                "AI extraction unavailable (ANTHROPIC_API_KEY not configured on ai-service) — "
                        + "please read the attached document manually.", true);
    }

    private static String nullableText(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return (v.isMissingNode() || v.isNull()) ? null : v.asText();
    }

    private static String stripCodeFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\n", "").replaceFirst("```\\s*$", "");
        }
        return t.trim();
    }
}
