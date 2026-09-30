package com.suraksha.backend.health.dto;

import com.suraksha.backend.health.MemberRelationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MemberRequest(
        @NotBlank(message = "Enter the member's full name.") @Size(max = 120) String fullName,
        @NotNull(message = "Enter the member's date of birth.") @Past(message = "Date of birth must be in the past.") LocalDate dateOfBirth,
        @NotNull(message = "Select the member's relationship to you.") MemberRelationship relationship,
        @Size(max = 500, message = "Keep declared conditions under 500 characters.") String preExistingConditions) {
}
