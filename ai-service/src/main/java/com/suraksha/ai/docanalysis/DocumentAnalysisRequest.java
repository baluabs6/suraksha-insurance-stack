package com.suraksha.ai.docanalysis;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentAnalysisRequest {
    @NotBlank(message = "Attach a photo or document to analyze.")
    private String imageBase64;

    private String mediaType;

    private String claimType;
    private String claimantDescription;
}
