package com.suraksha.ai.docanalysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Target type for Spring AI structured output when reading a claim document photo. */
public record ExtractedDocument(
        @JsonPropertyDescription("One short label such as 'medical bill', 'repair estimate', 'invoice' or 'unclear'")
        String documentType,
        @JsonPropertyDescription("Total amount shown, plain number with no currency symbol or commas, or null if not visible")
        String extractedAmount,
        @JsonPropertyDescription("Document date as YYYY-MM-DD if visible, otherwise null")
        String extractedDate,
        @JsonPropertyDescription("Hospital, garage or vendor name shown, or null")
        String merchantOrProvider,
        @JsonPropertyDescription("At most 8 short line-item descriptions; empty list if there is no itemised breakdown")
        List<String> lineItems,
        @JsonPropertyDescription("One short sentence flagging anything illegible, inconsistent or missing; empty string if nothing to flag")
        String notes
) {
}
