package com.suraksha.backend.documents.dto;

import com.suraksha.backend.documents.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DocumentUploadRequest {
    @NotNull(message = "Select the type of document.")
    private DocumentType documentType;

    @NotBlank(message = "File name is required.")
    private String fileName;

    @NotBlank(message = "Content type is required.")
    private String contentType;

    @NotBlank(message = "File content is required.")
    private String base64Content;

    private UUID policyId;
    private UUID claimId;
}
