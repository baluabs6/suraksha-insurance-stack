package com.suraksha.backend.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

@Service
public class TotpService {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final String HMAC_ALGO = "HmacSHA1";
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String buildOtpAuthUrl(String base32Secret, String accountEmail, String issuer) {
        String label = issuer + ":" + accountEmail;
        return "otpauth://totp/%s?secret=%s&issuer=%s&digits=%d&period=%d"
                .formatted(label, base32Secret, issuer, CODE_DIGITS, TIME_STEP_SECONDS);
    }

    public boolean verifyCode(String base32Secret, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long currentWindow = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;
        for (long window = currentWindow - 1; window <= currentWindow + 1; window++) {
            if (generateCode(base32Secret, window).equals(code)) {
                return true;
            }
        }
        return false;
    }

    private String generateCode(String base32Secret, long window) {
        try {
            byte[] key = base32Decode(base32Secret);
            byte[] data = new byte[8];
            long value = window;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xFF);
                value >>= 8;
            }
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(key, HMAC_ALGO));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate TOTP code", e);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int bits = 0;
        int value = 0;
        for (byte b : data) {
            value = (value << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(BASE32_ALPHABET.charAt((value >>> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            sb.append(BASE32_ALPHABET.charAt((value << (5 - bits)) & 31));
        }
        return sb.toString();
    }

    private byte[] base32Decode(String base32) {
        String clean = base32.trim().toUpperCase().replace("=", "");
        int bits = 0;
        int value = 0;
        int index = 0;
        byte[] output = new byte[clean.length() * 5 / 8];
        for (char c : clean.toCharArray()) {
            int idx = BASE32_ALPHABET.indexOf(c);
            if (idx < 0) continue;
            value = (value << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                output[index++] = (byte) ((value >>> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return output;
    }
}
