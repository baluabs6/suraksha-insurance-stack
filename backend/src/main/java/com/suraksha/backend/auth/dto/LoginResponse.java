package com.suraksha.backend.auth.dto;

import lombok.Value;

/** Either a full user session (mfaRequired=false, user populated) or a challenge (mfaRequired=true, mfaToken populated). */
@Value
public class LoginResponse {
    boolean mfaRequired;
    String mfaToken;
    UserResponse user;

    public static LoginResponse challenge(String mfaToken) {
        return new LoginResponse(true, mfaToken, null);
    }

    public static LoginResponse success(UserResponse user) {
        return new LoginResponse(false, null, user);
    }
}
