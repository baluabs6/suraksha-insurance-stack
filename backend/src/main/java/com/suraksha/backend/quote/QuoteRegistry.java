package com.suraksha.backend.quote;

import com.suraksha.backend.policy.PolicyType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.suraksha.backend.product.ClaimField.*;

/** Quote form for every insurance type. Rating rules live in {@link PremiumCalculator}. */
public final class QuoteRegistry {

    private static final Map<PolicyType, QuoteSpec> SPECS = new EnumMap<>(PolicyType.class);

    static {
        SPECS.put(PolicyType.HEALTH, new QuoteSpec("Sum insured", List.of(300000L, 500000L, 1000000L, 2500000L, 5000000L), null,
                List.of(number("eldestMemberAge", "Age of the eldest member to be covered", true).withHelp("18 to 75."),
                        number("membersCount", "Number of members to be covered", true).withHelp("1 to 6. You add their details after buying.")),
                Set.of(), "per year"));

        SPECS.put(PolicyType.LIFE, new QuoteSpec("Cover amount", List.of(2500000L, 5000000L, 10000000L, 20000000L), null,
                List.of(number("age", "Your age", true).withHelp("18 to 65."),
                        bool("smoker", "I smoke or use tobacco"),
                        select("termYears", "Policy term (years)", true, "10", "20", "30")),
                Set.of(), "per year"));

        SPECS.put(PolicyType.MOTOR, new QuoteSpec("Insured value", List.of(), "vehicleValue",
                List.of(number("vehicleValue", "Current value of the car (₹)", true),
                        number("vehicleAgeYears", "Age of the car (years)", true),
                        text("vehicleRegistrationNumber", "Registration number", true),
                        text("vehicleModel", "Make and model", false)),
                Set.of("vehicleRegistrationNumber", "vehicleModel"), "per year"));

        SPECS.put(PolicyType.TWO_WHEELER, new QuoteSpec("Insured value", List.of(), "vehicleValue",
                List.of(number("vehicleValue", "Current value of the vehicle (₹)", true),
                        number("vehicleAgeYears", "Age of the vehicle (years)", true),
                        select("engineCc", "Engine capacity", true, "Up to 75 cc", "76 to 150 cc", "151 to 350 cc", "Above 350 cc"),
                        text("vehicleRegistrationNumber", "Registration number", true),
                        text("vehicleModel", "Make and model", false)),
                Set.of("vehicleRegistrationNumber", "vehicleModel"), "per year"));

        SPECS.put(PolicyType.HOME, new QuoteSpec("Sum insured", List.of(1000000L, 2500000L, 5000000L, 10000000L), null,
                List.of(select("propertyType", "Type of property", true, "Apartment", "Independent house"),
                        text("propertyAddress", "Address of the property", true),
                        number("constructionAgeYears", "Age of the building (years)", true)),
                Set.of("propertyType", "propertyAddress"), "per year"));

        SPECS.put(PolicyType.TRAVEL, new QuoteSpec("Cover amount", List.of(250000L, 500000L, 1000000L, 2500000L), null,
                List.of(text("destination", "Destination", true),
                        select("destinationRegion", "Region", true, "Domestic", "Asia",
                                "Worldwide excluding USA and Canada", "Worldwide including USA and Canada"),
                        number("tripDays", "Trip length (days)", true).withHelp("1 to 180."),
                        number("travellers", "Number of travellers", true).withHelp("1 to 10.")),
                Set.of("destination", "destinationRegion", "tripDays", "travellers"), "for the whole trip"));

        SPECS.put(PolicyType.PERSONAL_ACCIDENT, new QuoteSpec("Cover amount", List.of(500000L, 1000000L, 2500000L, 5000000L), null,
                List.of(number("age", "Your age", true).withHelp("18 to 70."),
                        select("occupationRisk", "Occupation risk", true, "Low (desk job)", "Medium (field work)", "High (manual or hazardous work)")),
                Set.of(), "per year"));

        SPECS.put(PolicyType.CYBER, new QuoteSpec("Cover amount", List.of(50000L, 100000L, 250000L, 500000L), null,
                List.of(select("riskProfile", "How you use the internet", true,
                        "Basic (browsing, messaging)", "Frequent online shopping and UPI", "Freelancer or small business owner")),
                Set.of(), "per year"));

        SPECS.put(PolicyType.GADGET, new QuoteSpec("Insured value", List.of(), "deviceValue",
                List.of(select("deviceType", "Device", true, "Smartphone", "Laptop", "Tablet", "Smartwatch", "Camera"),
                        text("deviceModel", "Make and model", true),
                        number("deviceValue", "Purchase value (₹)", true).withHelp("₹3,000 to ₹3,00,000."),
                        number("deviceAgeMonths", "Age of the device (months)", true).withHelp("Up to 24 months.")),
                Set.of("deviceType", "deviceModel"), "per year"));

        SPECS.put(PolicyType.PET, new QuoteSpec("Cover amount", List.of(50000L, 100000L, 200000L), null,
                List.of(select("petType", "Pet", true, "Dog", "Cat"),
                        text("petName", "Pet's name", true),
                        text("breed", "Breed", false),
                        number("petAgeYears", "Pet's age (years)", true).withHelp("Up to 8.")),
                Set.of("petType", "petName", "breed"), "per year"));

        SPECS.put(PolicyType.BUSINESS, new QuoteSpec("Sum insured", List.of(500000L, 1000000L, 2500000L, 5000000L), null,
                List.of(text("businessName", "Business name", true),
                        select("businessType", "Type of business", true, "Retail shop", "Office", "Restaurant", "Small manufacturing", "Workshop"),
                        text("premisesAddress", "Premises address", true)),
                Set.of("businessName", "businessType", "premisesAddress"), "per year"));

        SPECS.put(PolicyType.MARINE_CARGO, new QuoteSpec("Cargo value", List.of(), "cargoValue",
                List.of(number("cargoValue", "Value of the shipment (₹)", true),
                        select("transitMode", "Mode of transit", true, "Sea", "Air", "Road", "Rail"),
                        text("originLocation", "Origin", true),
                        text("destinationLocation", "Destination", true)),
                Set.of("transitMode", "originLocation", "destinationLocation"), "for this shipment"));
    }

    private QuoteRegistry() {}

    public static QuoteSpec get(PolicyType type) {
        QuoteSpec spec = SPECS.get(type);
        if (spec == null) throw new IllegalStateException("No quote form registered for " + type);
        return spec;
    }

    public static Map<PolicyType, QuoteSpec> all() {
        return java.util.Collections.unmodifiableMap(SPECS);
    }
}
