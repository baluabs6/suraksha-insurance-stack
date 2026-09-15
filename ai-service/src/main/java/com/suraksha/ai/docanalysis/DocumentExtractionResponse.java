package com.suraksha.ai.docanalysis;

import java.util.List;

public record DocumentExtractionResponse(
        String documentType,
        String extractedAmount,
        String extractedDate,
        String merchantOrProvider,
        List<String> lineItems,
        String notes,
        boolean usedFallback
) {
}
