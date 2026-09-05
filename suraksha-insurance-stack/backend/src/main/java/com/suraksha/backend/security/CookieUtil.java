package com.suraksha.backend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    public void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        addAccessCookie(response, accessToken);
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true).secure(true).sameSite("Strict")
                .path("/api/auth").maxAge(Duration.ofDays(7)).build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public void addAccessCookie(HttpServletResponse response, String accessToken) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true).secure(true).sameSite("Strict")
                .path("/").maxAge(Duration.ofMinutes(15)).build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie clearAccess = ResponseCookie.from("access_token", "")
                .httpOnly(true).secure(true).sameSite("Strict").path("/").maxAge(0).build();
        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
                .httpOnly(true).secure(true).sameSite("Strict").path("/api/auth").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());
    }
}
