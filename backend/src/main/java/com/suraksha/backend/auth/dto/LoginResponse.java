package com.suraksha.backend.auth.dto;

import lombok.Value;

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
