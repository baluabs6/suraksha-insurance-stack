package com.suraksha.backend.product;

import com.suraksha.backend.policy.PolicyType;

import java.util.List;

/**
 * Everything the platform needs to know about one insurance line: how it is presented, and what a
 * claim against it must contain. Served to the frontend at GET /api/products.
 *
 * @param dedicatedClaimFlow true when the type has its own claim flow (health: insured members,
 *                           itemised bill, deterministic assessor) instead of the generic field-driven form
 */
public record ProductDefinition(PolicyType type, String label, String category, String icon,
                                String planName, String fromPrice, String tagline,
                                boolean dedicatedClaimFlow,
                                List<ClaimField> claimFields, List<String> requiredDocuments) {
}
