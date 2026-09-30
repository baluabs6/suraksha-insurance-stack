package com.suraksha.ai.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PiiRedactorTest {

    @Test
    void masksAadhaarAndPan() {
        String out = PiiRedactor.redact("Aadhaar 1234 5678 9012, PAN abcde1234f");
        assertEquals("Aadhaar [REDACTED_AADHAAR], PAN [REDACTED_PAN]", out);
    }

    @Test
    void masksLuhnValidCardAndKeepsSurroundingText() {
        assertEquals("card [REDACTED_CARD] ok", PiiRedactor.redact("card 4111 1111 1111 1111 ok"));
    }

    @Test
    void failsClosedOnLongDigitRunsThatAreNotCards() {
        assertFalse(PiiRedactor.redact("ref 1234 5678 9012 3456").contains("3456"));
    }

    @Test
    void masksEmailAndMobileNumbers() {
        String out = PiiRedactor.redact("mail a.b@example.co.in or call +91 98765 43210 / 9876543210");
        assertFalse(out.contains("@"));
        assertFalse(out.contains("98765"));
    }

    @Test
    void leavesPolicyNumbersAmountsDatesAndBracesAlone() {
        String text = "POL-HL-2201394 claim of 1250000 rupees on 2026-09-01 {name}";
        assertEquals(text, PiiRedactor.redact(text));
    }

    @Test
    void nullAndEmptyPassThrough() {
        assertEquals(null, PiiRedactor.redact(null));
        assertEquals("", PiiRedactor.redact(""));
    }
}
