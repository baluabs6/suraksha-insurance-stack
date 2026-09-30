package com.suraksha.ai.recommend;

import com.suraksha.ai.model.PolicyTypeView;

import java.util.List;
import java.util.Optional;

/**
 * Plans the AI can recommend or compare. Mirrors the backend's ProductRegistry (plan names and
 * one-line descriptions) — keep them aligned when a plan changes. Illustrative catalogue: confirm
 * names and terms with product and compliance before real use.
 */
public class PlanCatalog {

    public record Plan(PolicyTypeView type, String name, String pitchTemplate) {
    }

    public static final List<Plan> ALL = List.of(
            new Plan(PolicyTypeView.HEALTH, "CarePlus Family",
                    "Covers hospitalization for your whole family under one policy, with no sub-limit on room rent."),
            new Plan(PolicyTypeView.MOTOR, "DriveSecure Comprehensive",
                    "Zero-depreciation cover with a 60-minute cashless garage promise."),
            new Plan(PolicyTypeView.LIFE, "LifeShield Term 30",
                    "₹50 lakh term cover with a decision on your application within 48 hours."),
            new Plan(PolicyTypeView.TWO_WHEELER, "RideSafe Two-Wheeler",
                    "Own-damage and third-party cover for bikes and scooters, including theft and fire."),
            new Plan(PolicyTypeView.TRAVEL, "Wanderly Travel Cover",
                    "Trip cancellation, delays, lost baggage and medical emergencies abroad."),
            new Plan(PolicyTypeView.HOME, "HomeShield Property",
                    "Protects the structure and contents of your home against fire, flood, burglary and more."),
            new Plan(PolicyTypeView.PERSONAL_ACCIDENT, "SafeGuard Personal Accident",
                    "A lump sum or expense cover if an accident causes injury, disability or death."),
            new Plan(PolicyTypeView.CYBER, "NetSafe Cyber Cover",
                    "Financial loss from online fraud, identity theft and other cyber incidents."),
            new Plan(PolicyTypeView.GADGET, "GadgetCare Protection",
                    "Screen damage, liquid damage and theft cover for phones, laptops and more."),
            new Plan(PolicyTypeView.PET, "PawCare Pet Cover",
                    "Vet bills for illness and accidents, plus third-party liability for your dog or cat."),
            new Plan(PolicyTypeView.BUSINESS, "BizProtect Shop & Office",
                    "Fire, burglary, machinery breakdown and liability cover for small businesses."),
            new Plan(PolicyTypeView.MARINE_CARGO, "CargoSure Marine Transit",
                    "Goods in transit by sea, air, road or rail, covered against damage, loss and theft.")
    );

    public static Optional<Plan> forType(PolicyTypeView type) {
        return ALL.stream().filter(p -> p.type() == type).findFirst();
    }
}
