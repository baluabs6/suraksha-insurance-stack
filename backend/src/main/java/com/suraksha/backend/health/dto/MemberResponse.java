package com.suraksha.backend.health.dto;

import com.suraksha.backend.health.MemberRelationship;
import com.suraksha.backend.health.PolicyMember;

import java.time.LocalDate;
import java.util.UUID;

public record MemberResponse(UUID id, String fullName, LocalDate dateOfBirth,
                             MemberRelationship relationship, String preExistingConditions) {
    public static MemberResponse from(PolicyMember m) {
        return new MemberResponse(m.getId(), m.getFullName(), m.getDateOfBirth(),
                m.getRelationship(), m.getPreExistingConditions());
    }
}
