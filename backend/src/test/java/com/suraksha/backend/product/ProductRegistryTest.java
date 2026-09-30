package com.suraksha.backend.product;

import com.suraksha.backend.policy.PolicyType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductRegistryTest {

    @Test
    void everyPolicyTypeHasExactlyOneDefinition() {
        for (PolicyType type : PolicyType.values()) {
            assertEquals(type, ProductRegistry.get(type).type());
        }
        assertEquals(PolicyType.values().length, ProductRegistry.ALL.size());
    }

    @Test
    void definitionsAreWellFormed() {
        for (ProductDefinition p : ProductRegistry.ALL) {
            assertFalse(p.planName().isBlank(), p.type() + " needs a plan name");
            assertFalse(p.requiredDocuments().isEmpty(), p.type() + " needs a document checklist");
            if (!p.dedicatedClaimFlow()) {
                assertFalse(p.claimFields().isEmpty(), p.type() + " needs claim fields");
            }

            Set<String> keys = new HashSet<>();
            for (ClaimField f : p.claimFields()) {
                assertTrue(keys.add(f.key()), p.type() + " has a duplicate field key " + f.key());
                if (f.type().equals("select")) {
                    assertFalse(f.options().isEmpty(), p.type() + "." + f.key() + " needs options");
                }
            }
            for (ClaimField f : p.claimFields()) {
                if (f.requiredWhenKey() != null) {
                    assertTrue(keys.contains(f.requiredWhenKey()),
                            p.type() + "." + f.key() + " depends on a field that doesn't exist");
                }
            }
        }
    }

    @Test
    void healthKeepsItsDedicatedFlow() {
        assertTrue(ProductRegistry.get(PolicyType.HEALTH).dedicatedClaimFlow());
        assertEquals(List.of(), ProductRegistry.get(PolicyType.HEALTH).claimFields());
    }
}
