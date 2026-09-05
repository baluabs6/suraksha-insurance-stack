package com.suraksha.ai.docanalysis;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentAnalysisRequest {
    // Raw base64 payload, no "data:image/jpeg;base64," prefix — frontend
    // strips that before sending, same convention as everywhere else base64
    // is handled in this stack.
    @NotBlank(message = "Attach a photo or document to analyze.")
    private String imageBase64;

    // e.g. "image/jpeg", "image/png". Defaults to image/jpeg in the client
    // if left blank.
    private String mediaType;

    // Claim type (HEALTH/MOTOR/LIFE) and the claimant's own description —
    // used so the model can flag when the photo doesn't obviously match
    // what was claimed, not to help it decide whether to approve anything.
    private String claimType;
    private String claimantDescription;
}
