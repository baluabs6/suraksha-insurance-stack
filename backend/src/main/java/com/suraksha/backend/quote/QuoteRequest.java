package com.suraksha.backend.quote;

import com.suraksha.backend.policy.PolicyType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
public class QuoteRequest {
    @NotNull(message = "Choose the type of insurance.")
    private PolicyType type;

    /** Required when the type has coverage options; ignored when coverage comes from a value field. */
    private BigDecimal coverageAmount;

    /** Answers to the type's quote form (see GET /api/quotes/forms). */
    private Map<String, Object> inputs;

    /** Optional; today by default, at most 60 days ahead. */
    private LocalDate startDate;
}
