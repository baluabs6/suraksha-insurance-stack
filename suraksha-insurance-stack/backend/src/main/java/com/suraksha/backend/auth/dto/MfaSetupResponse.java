package com.suraksha.backend.auth.dto;

import lombok.Value;

@Value
public class MfaSetupResponse {
    String secret;
    String otpAuthUrl;
}
