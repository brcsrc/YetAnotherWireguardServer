package com.brcsrc.yaws.twofa.totp;

import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.twofa.crypto.TwoFactorCryptoService;
import org.apache.commons.codec.binary.Base32;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Service
public class TotpService {

    private final Base32 base32 = new Base32();
    private final SecureRandom secureRandom = new SecureRandom();
    private final TwoFactorCryptoService twoFactorCryptoService;

    public TotpService(TwoFactorCryptoService twoFactorCryptoService) {
        this.twoFactorCryptoService = twoFactorCryptoService;
    }

    public String generateBase32Secret() {
        byte[] randomBytes = new byte[20];
        secureRandom.nextBytes(randomBytes);
        return base32.encodeToString(randomBytes).replace("=", "").toUpperCase(Locale.ROOT);
    }

    public String buildOtpAuthUri(String issuer, String accountName, String base32Secret) {
        String encodedIssuer = urlEncode(issuer);
        String encodedAccountName = urlEncode(accountName);
        String encodedSecret = urlEncode(base32Secret);
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                encodedIssuer,
                encodedAccountName,
                encodedSecret,
                encodedIssuer
        );
    }

    public String encryptSecret(String base32Secret) {
        return twoFactorCryptoService.encrypt(base32Secret);
    }

    public String decryptSecret(String encryptedSecret) {
        return twoFactorCryptoService.decrypt(encryptedSecret);
    }

    /**
     * Validates a TOTP code and returns the matched counter value, or empty if invalid.
     * Callers should persist the returned counter and reject any future submission that
     * matches the same counter (replay prevention).
     */
    public java.util.OptionalLong validateCodeAndGetCounter(String encryptedSecret, String otpCode, Instant now) {
        String secret = decryptSecret(encryptedSecret);
        byte[] decodedSecret = base32.decode(secret);

        long currentWindow = now.getEpochSecond() / 30;
        for (int offset = -1; offset <= 1; offset++) {
            long counter = currentWindow + offset;
            String generatedCode = generateTotpCode(decodedSecret, counter);
            if (Objects.equals(generatedCode, otpCode)) {
                return java.util.OptionalLong.of(counter);
            }
        }
        return java.util.OptionalLong.empty();
    }

    /** @deprecated Use {@link #validateCodeAndGetCounter} to enable replay prevention. */
    @Deprecated
    public boolean isValidCode(String encryptedSecret, String otpCode, Instant now) {
        return validateCodeAndGetCounter(encryptedSecret, otpCode, now).isPresent();
    }

    public void validateOtpFormatOrThrow(String otpCode) {
        if (otpCode == null || !otpCode.matches(Constants.SIX_DIGIT_NUMERIC_CODE_REGEX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "otp code must be a 6 digit numeric value");
        }
    }

    private String generateTotpCode(byte[] key, long counter) {
        try {
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
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to generate OTP");
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
