package com.suraksha.ai.model;

/**
 * Mirror of the backend's PolicyType. Keep the two in sync: the ai-service reads the shared
 * policies table, and an enum value it doesn't know would fail the read.
 */
public enum PolicyTypeView {
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
