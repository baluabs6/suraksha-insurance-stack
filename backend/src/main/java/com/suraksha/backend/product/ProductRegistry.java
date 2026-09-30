package com.suraksha.backend.product;

import com.suraksha.backend.policy.PolicyType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.suraksha.backend.product.ClaimField.*;

/**
 * Catalogue of insurance lines. Plan names, prices and claim fields are illustrative starting points —
 * confirm them with your product, actuarial and compliance teams (and IRDAI-approved product terms)
 * before any real use. Premiums and payouts are never produced by the AI layer.
 */
public final class ProductRegistry {

    private static final List<String> VEHICLE_INCIDENTS =
            List.of("Accident", "Theft", "Fire", "Natural calamity", "Third-party liability");

    private static List<ClaimField> vehicleFields() {
        return List.of(
                text("vehicleRegistrationNumber", "Vehicle registration number", true),
                select("incidentType", "What happened", true, VEHICLE_INCIDENTS.toArray(String[]::new)),
                text("policeReportNumber", "FIR / police report number", false)
                        .requiredWhen("incidentType", "Theft")
                        .withHelp("Required for theft claims."),
                bool("thirdPartyInvolved", "A third party was involved or injured"),
                text("garageName", "Garage or workshop (if already taken there)", false));
    }

    public static final List<ProductDefinition> ALL = List.of(
            new ProductDefinition(PolicyType.HEALTH, "Health", "Health & Life", "heart-pulse",
                    "CarePlus Family", "₹649/month",
                    "Covers the whole family under one policy, no sub-limits on room rent.",
                    true, List.of(),
                    List.of("Hospital bills and discharge summary", "Doctor's prescription and investigation reports",
                            "Photo ID of the insured member")),
            new ProductDefinition(PolicyType.LIFE, "Life (Term)", "Health & Life", "umbrella",
                    "LifeShield Term 30", "₹899/month",
                    "₹50 lakh cover with a decision on your application within 48 hours.",
                    false,
                    List.of(select("eventType", "Type of claim", true, "Death", "Permanent disability"),
                            select("claimantRelationship", "Claimant's relationship to the insured", true,
                                    "Nominee", "Spouse", "Child", "Parent", "Legal heir")),
                    List.of("Original policy document", "Death or disability certificate", "Claimant's ID and bank details")),
            new ProductDefinition(PolicyType.MOTOR, "Car", "Vehicle", "car",
                    "DriveSecure Comprehensive", "₹399/month",
                    "Zero depreciation cover with a 60-minute garage cashless promise.",
                    false, vehicleFields(),
                    List.of("Registration certificate and driving licence", "Photos of the damage",
                            "Repair estimate", "FIR for theft or third-party injury")),
            new ProductDefinition(PolicyType.TWO_WHEELER, "Two-wheeler", "Vehicle", "bike",
                    "RideSafe Two-Wheeler", "₹99/month",
                    "Own-damage and third-party cover for bikes and scooters, including theft and fire.",
                    false, vehicleFields(),
                    List.of("Registration certificate and driving licence", "Photos of the damage",
                            "Repair estimate", "FIR for theft")),
            new ProductDefinition(PolicyType.HOME, "Home & property", "Home & Property", "home",
                    "HomeShield Property", "₹250/month",
                    "Protects the structure and contents of your home against fire, flood, burglary and more.",
                    false,
                    List.of(select("claimCategory", "What happened", true,
                                    "Fire", "Water damage or flooding", "Burglary or theft", "Natural calamity", "Structural damage"),
                            text("propertyAddress", "Address of the property", true),
                            text("itemsAffected", "Structure or items affected", true),
                            text("policeReportNumber", "FIR / police report number", false)
                                    .requiredWhen("claimCategory", "Burglary or theft")
                                    .withHelp("Required for burglary or theft claims.")),
                    List.of("Photos or video of the damage", "Repair or replacement estimates",
                            "Purchase invoices for affected items", "FIR for burglary or theft")),
            new ProductDefinition(PolicyType.TRAVEL, "Travel", "Travel & Accident", "plane",
                    "Wanderly Travel Cover", "₹149/trip",
                    "Trip cancellation, delays, lost baggage and medical emergencies abroad.",
                    false,
                    List.of(select("claimCategory", "What happened", true,
                                    "Trip cancellation", "Flight delay", "Baggage loss or delay",
                                    "Medical emergency abroad", "Passport loss"),
                            text("destination", "Destination", true),
                            date("departureDate", "Departure date", true),
                            date("returnDate", "Return date", false),
                            text("carrierName", "Airline or carrier", false)),
                    List.of("Tickets and boarding passes", "Carrier's delay or baggage report",
                            "Medical bills and reports for emergencies abroad", "Passport copy")),
            new ProductDefinition(PolicyType.PERSONAL_ACCIDENT, "Personal accident", "Travel & Accident", "activity",
                    "SafeGuard Personal Accident", "₹199/month",
                    "A lump sum or expense cover if an accident causes injury, disability or death.",
                    false,
                    List.of(select("injuryType", "Outcome of the accident", true,
                                    "Accidental death", "Permanent total disability", "Permanent partial disability",
                                    "Temporary disability", "Medical expenses"),
                            text("accidentLocation", "Where it happened", true),
                            text("hospitalName", "Hospital (if treated)", false),
                            text("policeReportNumber", "FIR / police report number", false)),
                    List.of("Medical reports and discharge summary", "FIR or police report, if any",
                            "Disability certificate for disability claims")),
            new ProductDefinition(PolicyType.CYBER, "Cyber", "Digital & Devices", "shield-alert",
                    "NetSafe Cyber Cover", "₹99/month",
                    "Financial loss from online fraud, identity theft and other cyber incidents.",
                    false,
                    List.of(select("incidentType", "What happened", true,
                                    "Online or UPI fraud", "Identity theft", "Ransomware or extortion",
                                    "Data breach", "Online harassment"),
                            text("platformInvolved", "App, website or platform involved", false),
                            bool("reportedToCyberCell", "Reported to the cyber crime portal or police"),
                            text("cyberCrimeReportNumber", "Cyber crime complaint number", false)
                                    .requiredWhen("reportedToCyberCell", "true")),
                    List.of("Screenshots and transaction records", "Bank statement showing the loss",
                            "Cyber crime complaint acknowledgement")),
            new ProductDefinition(PolicyType.GADGET, "Gadget", "Digital & Devices", "smartphone",
                    "GadgetCare Protection", "₹129/month",
                    "Screen damage, liquid damage and theft cover for phones, laptops and more.",
                    false,
                    List.of(select("deviceType", "Device", true, "Smartphone", "Laptop", "Tablet", "Smartwatch", "Camera"),
                            text("deviceModel", "Make and model", true),
                            text("serialOrImei", "Serial number or IMEI", true),
                            select("damageType", "What happened", true,
                                    "Screen damage", "Liquid damage", "Accidental damage", "Theft", "Fire"),
                            text("policeReportNumber", "FIR / police report number", false)
                                    .requiredWhen("damageType", "Theft")),
                    List.of("Photos of the damage", "Purchase invoice", "Repair estimate", "FIR for theft")),
            new ProductDefinition(PolicyType.PET, "Pet", "Digital & Devices", "paw-print",
                    "PawCare Pet Cover", "₹299/month",
                    "Vet bills for illness and accidents, plus third-party liability for your dog or cat.",
                    false,
                    List.of(select("petType", "Pet", true, "Dog", "Cat"),
                            text("petName", "Pet's name", true),
                            select("incidentType", "What happened", true,
                                    "Illness", "Accident", "Surgery", "Third-party liability", "Death"),
                            text("veterinaryClinic", "Veterinary clinic", true)),
                    List.of("Vet bills and prescriptions", "Vaccination record", "Veterinary certificate")),
            new ProductDefinition(PolicyType.BUSINESS, "Business", "Business", "briefcase",
                    "BizProtect Shop & Office", "₹499/month",
                    "Fire, burglary, machinery breakdown and liability cover for small businesses.",
                    false,
                    List.of(text("businessName", "Business name", true),
                            select("incidentType", "What happened", true,
                                    "Fire", "Burglary", "Business interruption", "Public liability",
                                    "Professional liability", "Machinery breakdown"),
                            text("premisesAddress", "Premises address", false),
                            bool("thirdPartyInvolved", "A third party was injured or their property damaged")),
                    List.of("Photos of the damage", "Stock and asset records", "FIR for burglary",
                            "Third-party claim notice, if any")),
            new ProductDefinition(PolicyType.MARINE_CARGO, "Marine cargo", "Business", "ship",
                    "CargoSure Marine Transit", "Quote per shipment",
                    "Goods in transit by sea, air, road or rail, covered against damage, loss and theft.",
                    false,
                    List.of(text("shipmentReference", "Bill of lading / LR / airway bill number", true),
                            select("transitMode", "Mode of transit", true, "Sea", "Air", "Road", "Rail"),
                            text("originLocation", "Origin", true),
                            text("destinationLocation", "Destination", true),
                            select("lossType", "What happened", true, "Damage", "Loss or shortage", "Theft or pilferage"),
                            text("surveyorReference", "Surveyor report reference", false)),
                    List.of("Bill of lading / consignment note", "Commercial invoice and packing list",
                            "Survey report", "Notice of claim to the carrier"))
    );

    private static final Map<PolicyType, ProductDefinition> BY_TYPE =
            ALL.stream().collect(Collectors.toUnmodifiableMap(ProductDefinition::type, Function.identity()));

    private ProductRegistry() {}

    public static ProductDefinition get(PolicyType type) {
        ProductDefinition definition = BY_TYPE.get(type);
        if (definition == null) {
            throw new IllegalStateException("No ProductDefinition registered for policy type " + type);
        }
        return definition;
    }
}
