package com.suraksha.backend.quote;

import com.suraksha.backend.policy.PolicyType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Transparent, rule-based premium rating. The AI layer never computes a premium.
 *
 * ILLUSTRATIVE RATES ONLY: the constants below are demo values, not filed or actuarially derived rates.
 * Replace them with your approved rating tables before any real use.
 *
 * Every line is a whole-rupee amount and the premium is exactly their sum, so the breakdown shown to the
 * customer always adds up.
 */
public final class PremiumCalculator {

    public record Line(String label, BigDecimal amount) {}

    public record Quote(BigDecimal premium, List<Line> lines) {}

    private PremiumCalculator() {}

    public static Quote calculate(PolicyType type, BigDecimal coverage, Map<String, Object> in) {
        double cov = coverage.doubleValue();
        return switch (type) {
            case HEALTH -> health(cov, in);
            case LIFE -> life(cov, in);
            case MOTOR -> motor(in);
            case TWO_WHEELER -> twoWheeler(in);
            case HOME -> home(cov, in);
            case TRAVEL -> travel(cov, in);
            case PERSONAL_ACCIDENT -> personalAccident(cov, in);
            case CYBER -> cyber(cov, in);
            case GADGET -> gadget(in);
            case PET -> pet(cov, in);
            case BUSINESS -> business(cov, in);
            case MARINE_CARGO -> marine(in);
        };
    }

    // ---------------------------------------------------------------- health, life

    private static Quote health(double cov, Map<String, Object> in) {
        int age = intOf(in, "eldestMemberAge");
        int members = intOf(in, "membersCount");
        require(age >= 18 && age <= 75, "Health cover is available when the eldest member is 18 to 75 years old.");
        require(members >= 1 && members <= 6, "A health policy can cover 1 to 6 members.");

        double ageFactor = age <= 25 ? 0.8 : age <= 35 ? 1.0 : age <= 45 ? 1.3 : age <= 55 ? 1.8 : age <= 65 ? 2.6 : 3.5;
        Builder b = new Builder().base("Base premium (1.2% of sum insured)", cov * 0.012)
                .factor("Age loading (eldest member " + age + ")", ageFactor);
        if (members > 1) {
            b.factor("Family cover (" + members + " members)", 1 + 0.55 * (members - 1));
        }
        return b.build();
    }

    private static Quote life(double cov, Map<String, Object> in) {
        int age = intOf(in, "age");
        int term = Integer.parseInt(str(in, "termYears"));
        require(age >= 18 && age <= 65, "Term cover is available for ages 18 to 65.");
        require(age + term <= 85, "The policy term must end by age 85.");

        double ratePer1000 = age <= 25 ? 0.9 : age <= 30 ? 1.1 : age <= 35 ? 1.4 : age <= 40 ? 2.0
                : age <= 45 ? 3.2 : age <= 50 ? 5.4 : age <= 55 ? 9.0 : 15.0;
        Builder b = new Builder().base("Base premium (age " + age + ")", cov / 1000 * ratePer1000);
        if (bool(in, "smoker")) b.factor("Tobacco loading", 1.7);
        b.factor("Term of " + term + " years", term == 10 ? 0.95 : term == 20 ? 1.0 : 1.08);
        return b.build();
    }

    // ---------------------------------------------------------------- vehicles

    private static Quote motor(Map<String, Object> in) {
        double value = numOf(in, "vehicleValue");
        int age = intOf(in, "vehicleAgeYears");
        require(value >= 50_000 && value <= 50_000_000, "Car value must be between ₹50,000 and ₹5,00,00,000.");
        require(age >= 0 && age <= 20, "Cars older than 20 years can't be insured online.");
        return new Builder().base("Own damage (2.5% of insured value)", value * 0.025)
                .factor("Vehicle age loading (" + age + " years)", vehicleAgeFactor(age))
                .flat("Third-party liability (illustrative)", 2500)
                .build();
    }

    private static Quote twoWheeler(Map<String, Object> in) {
        double value = numOf(in, "vehicleValue");
        int age = intOf(in, "vehicleAgeYears");
        String cc = str(in, "engineCc");
        require(value >= 20_000 && value <= 3_000_000, "Vehicle value must be between ₹20,000 and ₹30,00,000.");
        require(age >= 0 && age <= 20, "Vehicles older than 20 years can't be insured online.");
        double tp = cc.startsWith("Up to") ? 500 : cc.startsWith("76") ? 720 : cc.startsWith("151") ? 985 : 2300;
        return new Builder().base("Own damage (3% of insured value)", value * 0.03)
                .factor("Vehicle age loading (" + age + " years)", vehicleAgeFactor(age))
                .flat("Third-party liability, " + cc + " (illustrative)", tp)
                .build();
    }

    private static double vehicleAgeFactor(int age) {
        return age <= 1 ? 1.0 : age <= 3 ? 1.10 : age <= 5 ? 1.25 : age <= 10 ? 1.50 : 1.80;
    }

    // ---------------------------------------------------------------- property, travel, accident

    private static Quote home(double cov, Map<String, Object> in) {
        int buildingAge = intOf(in, "constructionAgeYears");
        require(buildingAge >= 0 && buildingAge <= 60, "Buildings older than 60 years can't be insured online.");
        Builder b = new Builder().base("Base premium (0.12% of sum insured)", cov * 0.0012);
        if ("Independent house".equalsIgnoreCase(str(in, "propertyType"))) b.factor("Independent house", 1.25);
        if (buildingAge > 20) b.factor("Building age over 20 years", 1.2);
        else if (buildingAge > 10) b.factor("Building age over 10 years", 1.1);
        return b.build();
    }

    private static Quote travel(double cov, Map<String, Object> in) {
        int days = intOf(in, "tripDays");
        int travellers = intOf(in, "travellers");
        require(days >= 1 && days <= 180, "Trips can be 1 to 180 days long.");
        require(travellers >= 1 && travellers <= 10, "A travel policy can cover 1 to 10 travellers.");

        String region = str(in, "destinationRegion");
        double perDay = region.equals("Domestic") ? 60 : region.equals("Asia") ? 180
                : region.startsWith("Worldwide excluding") ? 320 : 650;
        double coverFactor = cov <= 250_000 ? 0.75 : cov <= 500_000 ? 1.0 : cov <= 1_000_000 ? 1.6 : 2.4;
        return new Builder()
                .base(days + " days × " + travellers + " traveller(s), " + region, perDay * days * travellers)
                .factor("Cover level adjustment", coverFactor)
                .minimum(149)
                .build();
    }

    private static Quote personalAccident(double cov, Map<String, Object> in) {
        int age = intOf(in, "age");
        require(age >= 18 && age <= 70, "Personal accident cover is available for ages 18 to 70.");
        String occupation = str(in, "occupationRisk");
        double occupationFactor = occupation.startsWith("Low") ? 1.0 : occupation.startsWith("Medium") ? 1.4 : 2.2;
        Builder b = new Builder().base("Base premium (0.15% of cover)", cov * 0.0015)
                .factor("Occupation risk", occupationFactor);
        if (age > 60) b.factor("Age over 60", 1.3);
        return b.build();
    }

    // ---------------------------------------------------------------- digital, devices, pets, business

    private static Quote cyber(double cov, Map<String, Object> in) {
        String profile = str(in, "riskProfile");
        double factor = profile.startsWith("Basic") ? 1.0 : profile.startsWith("Frequent") ? 1.3 : 1.7;
        return new Builder().base("Base premium (1.2% of cover)", cov * 0.012).factor("Usage profile", factor).build();
    }

    private static Quote gadget(Map<String, Object> in) {
        double value = numOf(in, "deviceValue");
        int ageMonths = intOf(in, "deviceAgeMonths");
        require(value >= 3_000 && value <= 300_000, "Device value must be between ₹3,000 and ₹3,00,000.");
        require(ageMonths >= 0 && ageMonths <= 24, "Devices older than 24 months can't be insured.");

        String device = str(in, "deviceType");
        double deviceFactor = switch (device) {
            case "Laptop" -> 0.95;
            case "Tablet", "Smartwatch" -> 0.9;
            case "Camera" -> 0.85;
            default -> 1.0;
        };
        Builder b = new Builder().base("Base premium (6% of device value)", value * 0.06);
        b.factor(device, deviceFactor);
        if (ageMonths > 12) b.factor("Device older than 12 months", 1.2);
        return b.build();
    }

    private static Quote pet(double cov, Map<String, Object> in) {
        int age = intOf(in, "petAgeYears");
        require(age >= 0 && age <= 8, "Pets older than 8 years can't be insured online.");
        boolean dog = "Dog".equalsIgnoreCase(str(in, "petType"));
        Builder b = new Builder().base((dog ? "Dog" : "Cat") + " base premium for this cover", (dog ? 3500 : 2500) * cov / 100_000);
        if (age > 5) b.factor("Pet older than 5 years", 1.5);
        else if (age > 2) b.factor("Pet older than 2 years", 1.2);
        return b.build();
    }

    private static Quote business(double cov, Map<String, Object> in) {
        String type = str(in, "businessType");
        double factor = switch (type) {
            case "Office" -> 0.8;
            case "Restaurant" -> 1.5;
            case "Small manufacturing" -> 1.9;
            case "Workshop" -> 1.6;
            default -> 1.0;
        };
        return new Builder().base("Base premium (0.3% of sum insured)", cov * 0.003).factor(type, factor).build();
    }

    private static Quote marine(Map<String, Object> in) {
        double value = numOf(in, "cargoValue");
        require(value >= 10_000 && value <= 500_000_000, "Shipment value must be between ₹10,000 and ₹50,00,00,000.");
        String mode = str(in, "transitMode");
        double factor = mode.equals("Air") ? 0.6 : mode.equals("Road") ? 1.2 : mode.equals("Rail") ? 0.9 : 1.0;
        return new Builder().base("Base premium (0.25% of shipment value)", value * 0.0025)
                .factor("Transit by " + mode.toLowerCase(), factor)
                .minimum(500)
                .build();
    }

    // ---------------------------------------------------------------- helpers

    private static final class Builder {
        private final List<Line> lines = new ArrayList<>();
        private BigDecimal total = BigDecimal.ZERO;

        Builder base(String label, double amount) {
            return add(label, whole(amount));
        }

        Builder flat(String label, double amount) {
            return add(label, whole(amount));
        }

        /** Multiplies the running total; shown as the extra amount (negative for a discount). */
        Builder factor(String label, double multiplier) {
            BigDecimal delta = total.multiply(BigDecimal.valueOf(multiplier - 1)).setScale(0, RoundingMode.HALF_UP);
            return delta.signum() == 0 ? this : add(label, delta);
        }

        Builder minimum(double min) {
            BigDecimal shortfall = whole(min).subtract(total);
            return shortfall.signum() > 0 ? add("Minimum premium", shortfall) : this;
        }

        private Builder add(String label, BigDecimal amount) {
            lines.add(new Line(label, amount));
            total = total.add(amount);
            return this;
        }

        Quote build() {
            return new Quote(total, List.copyOf(lines));
        }

        private static BigDecimal whole(double value) {
            return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private static double numOf(Map<String, Object> in, String key) {
        Object v = in.get(key);
        if (v == null) throw new IllegalArgumentException("Missing " + key + ".");
        return new BigDecimal(v.toString()).doubleValue();
    }

    private static int intOf(Map<String, Object> in, String key) {
        return (int) Math.round(numOf(in, key));
    }

    private static String str(Map<String, Object> in, String key) {
        Object v = in.get(key);
        if (v == null) throw new IllegalArgumentException("Missing " + key + ".");
        return v.toString();
    }

    private static boolean bool(Map<String, Object> in, String key) {
        return Boolean.TRUE.equals(in.get(key));
    }
}
