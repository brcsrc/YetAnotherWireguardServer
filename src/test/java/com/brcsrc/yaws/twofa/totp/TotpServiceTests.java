package com.brcsrc.yaws.twofa.totp;

import com.brcsrc.yaws.twofa.crypto.TwoFactorCryptoService;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TotpServiceTests {

    @Mock
    private TwoFactorCryptoService twoFactorCryptoService;

    private TotpService totpService;

    @BeforeEach
    public void setup() {
        totpService = new TotpService(twoFactorCryptoService);
    }

    @Test
    public void generateBase32SecretReturnsUppercaseBase32String() {
        String secret = totpService.generateBase32Secret();

        assertNotNull(secret);
        assertTrue(secret.matches("^[A-Z2-7]+$"));
        assertFalse(secret.contains("="));
        assertTrue(secret.length() >= 16);
    }

    @Test
    public void buildOtpAuthUriReturnsExpectedFormat() {
        String uri = totpService.buildOtpAuthUri("YAWS", "admin", "ABCDEF123");
        assertEquals("otpauth://totp/YAWS:admin?secret=ABCDEF123&issuer=YAWS", uri);
    }

    @Test
    public void buildOtpAuthUriEncodesIssuerAndAccountName() {
        String uri = totpService.buildOtpAuthUri("YAWS Prod", "admin@example.com", "ABCDEF123");
        assertEquals(
                "otpauth://totp/YAWS%20Prod:admin%40example.com?secret=ABCDEF123&issuer=YAWS%20Prod",
                uri
        );
    }

    @Test
    public void validateOtpFormatOrThrowRejectsInvalidValues() {
        ResponseStatusException nullException = assertThrows(ResponseStatusException.class,
                () -> totpService.validateOtpFormatOrThrow(null));
        assertEquals(HttpStatus.BAD_REQUEST, nullException.getStatusCode());

        ResponseStatusException shortException = assertThrows(ResponseStatusException.class,
                () -> totpService.validateOtpFormatOrThrow("12345"));
        assertEquals(HttpStatus.BAD_REQUEST, shortException.getStatusCode());

        ResponseStatusException alphaException = assertThrows(ResponseStatusException.class,
                () -> totpService.validateOtpFormatOrThrow("12A456"));
        assertEquals(HttpStatus.BAD_REQUEST, alphaException.getStatusCode());
    }

    @Test
    public void isValidCodeAcceptsCurrentAndAdjacentWindowCodes() throws Exception {
        String base32Secret = "JBSWY3DPEHPK3PXP";
        String encrypted = "encrypted";
        when(twoFactorCryptoService.decrypt(encrypted)).thenReturn(base32Secret);

        Instant now = Instant.parse("2026-02-18T15:00:30Z");
        long currentWindow = now.getEpochSecond() / 30;
        byte[] decodedSecret = new Base32().decode(base32Secret);

        String currentCode = generateTotpCode(decodedSecret, currentWindow);
        String previousWindowCode = generateTotpCode(decodedSecret, currentWindow - 1);

        assertTrue(totpService.isValidCode(encrypted, currentCode, now));
        assertTrue(totpService.isValidCode(encrypted, previousWindowCode, now));
        assertFalse(totpService.isValidCode(encrypted, "000000", now));
    }

    private String generateTotpCode(byte[] key, long counter) throws Exception {
        byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hmac = mac.doFinal(data);
        int offset = hmac[hmac.length - 1] & 0x0F;
        int binary = ((hmac[offset] & 0x7F) << 24)
                | ((hmac[offset + 1] & 0xFF) << 16)
                | ((hmac[offset + 2] & 0xFF) << 8)
                | (hmac[offset + 3] & 0xFF);
        int otp = binary % 1_000_000;
        return String.format("%06d", otp);
    }
}
