package com.suraksha.backend.policy;

/**
 * Every insurance line the platform sells. Adding a value here also requires:
 *  1. a ProductDefinition in {@code product/ProductRegistry} (ProductRegistryTest fails otherwise),
 *  2. a quote form in {@code quote/QuoteRegistry} and a rating rule in {@code PremiumCalculator}, and
 *  3. the same value in ai-service's PolicyTypeView and PlanCatalog (reading an unknown enum value would throw).
 * The column has no CHECK constraint (see Policy and migration V2), so no schema change is needed.
 */
public enum PolicyType {
    HEALTH,
    MOTOR,
    LIFE,
    TWO_WHEELER,
    TRAVEL,
    HOME,
    PERSONAL_ACCIDENT,
    CYBER,
    GADGET,
    PET,
    MARINE_CARGO,
    BUSINESS
}
