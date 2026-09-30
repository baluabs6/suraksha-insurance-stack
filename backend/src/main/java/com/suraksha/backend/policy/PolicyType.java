package com.suraksha.backend.policy;

/**
 * Every insurance line the platform sells. Adding a value here also requires:
 *  1. a ProductDefinition in {@code product/ProductRegistry} (ProductRegistryTest fails otherwise),
 *  2. the same value in ai-service's PolicyTypeView (reading an unknown enum value would throw), and
 *  3. an existing database dropping the old check constraint: see db/policy-types-migration.sql.
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
