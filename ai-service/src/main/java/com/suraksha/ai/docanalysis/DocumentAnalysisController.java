package com.suraksha.ai.docanalysis;

import com.suraksha.ai.client.AnthropicClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

            Rules:
            - Extract only what is actually visible in the image. Never
              invent an amount, date, or name that isn't shown.
            - lineItems should list at most 8 items; use an empty array if the
              document has no itemized breakdown.
            - Never state or imply whether the claim should be approved,
              denied, or looks fraudulent — that is a human adjuster's job.
            """;

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

    @PostMapping("/extract")
    public DocumentExtractionResponse extract(@Valid @RequestBody DocumentAnalysisRequest req) {
        String claimType = req.getClaimType() == null || req.getClaimType().isBlank()
                ? "unspecified" : req.getClaimType();

        String userMessage = "Claim type: " + claimType + "\n\nExtract the structured fields from the attached image.";

        return anthropicClient.completeStructuredWithImage(
                        EXTRACTION_SYSTEM_PROMPT, userMessage, req.getImageBase64(), req.getMediaType(), ExtractedDocument.class)
                .map(doc -> new DocumentExtractionResponse(
                        doc.documentType() == null ? "unclear" : doc.documentType(),
                        doc.extractedAmount(), doc.extractedDate(), doc.merchantOrProvider(),
                        doc.lineItems() == null ? List.of() : doc.lineItems(),
                        doc.notes() == null ? "" : doc.notes(),
                        false))
                .orElseGet(this::fallbackExtraction);
    }

    private DocumentExtractionResponse fallbackExtraction() {
        return new DocumentExtractionResponse("unclear", null, null, null, List.of(),
                "AI extraction unavailable (ANTHROPIC_API_KEY not configured on ai-service) — "
                        + "please read the attached document manually.", true);
    }


}
