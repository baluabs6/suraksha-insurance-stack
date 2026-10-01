package com.suraksha.backend.quote;

import com.suraksha.backend.product.ClaimField;

import java.util.List;
import java.util.Set;

/**
 * What a customer is asked for when getting a quote for one insurance type.
 *
 * @param coverageLabel     label for the coverage selector ("Sum insured", "Cover amount"...)
 * @param coverageOptions   selectable coverage amounts; empty when coverage comes from a value field instead
 * @param coverageFromField when set, the coverage equals that input (vehicle value, device value, cargo value)
 * @param fields            rating and identification inputs (rendered as a form and validated like claim details)
 * @param attributeKeys     the inputs kept on the policy; everything else (age, smoker...) is used for rating only
 *                          and is not stored
 * @param premiumBasis      how the premium is charged, shown next to the price
 */
public record QuoteSpec(String coverageLabel, List<Long> coverageOptions, String coverageFromField,
                        List<ClaimField> fields, Set<String> attributeKeys, String premiumBasis) {
}
