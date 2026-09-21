package com.suraksha.ai.recommend;

import com.suraksha.ai.model.PolicyTypeView;

import java.util.List;

public class PlanCatalog {

    public record Plan(PolicyTypeView type, String name, String pitchTemplate) {
    }

    public static final List<Plan> ALL = List.of(
            new Plan(PolicyTypeView.HEALTH, "CarePlus Family",
                    "Covers hospitalization for your whole family under one policy, with no sub-limit on room rent."),
            new Plan(PolicyTypeView.MOTOR, "DriveSecure Comprehensive",
                    "Zero-depreciation cover with a 60-minute cashless garage promise."),
            new Plan(PolicyTypeView.LIFE, "LifeShield Term 30",
                    "₹50 lakh term cover with a decision on your application within 48 hours.")
    );
}
