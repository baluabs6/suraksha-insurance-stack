package com.suraksha.backend.auth.dto;

import com.suraksha.backend.user.Role;
import com.suraksha.backend.user.User;
import lombok.Value;

import java.util.UUID;

@Value
public class UserResponse {
    UUID id;
    String fullName;
    String email;
    Role role;
    boolean kycVerified;
    boolean mfaEnabled;

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.isKycVerified(), user.isMfaEnabled());
    }
}
