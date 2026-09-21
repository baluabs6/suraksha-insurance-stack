package com.suraksha.ai.policycompare;

import com.suraksha.ai.model.PolicyTypeView;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PolicyComparisonRequest {
    @NotBlank(message = "Describe your situation.")
    private String situation;

    private List<PolicyTypeView> ownedTypes;
}
