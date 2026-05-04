package com.brcsrc.yaws.twofa.crypto;

import com.brcsrc.yaws.exceptions.InternalServerException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TwoFactorCryptoService {

    private static final String TOTP_ENCRYPTION_KEY_BASE64 = "TOTP_ENCRYPTION_KEY_BASE64";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception e) {
            throw new InternalServerException("failed to encrypt two-factor secret");
        }
    }

    public String decrypt(String ciphertext) {
        try {
            String[] parts = ciphertext.split(":", 2);
            if (parts.length != 2) {
                throw new InternalServerException("invalid encrypted two-factor secret format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encryptedBytes = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(encryptedBytes);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (InternalServerException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException("failed to decrypt two-factor secret");
        }
    }

    private SecretKeySpec getSecretKeySpec() {
        String base64Key = System.getenv(TOTP_ENCRYPTION_KEY_BASE64);
        if (base64Key == null || base64Key.isBlank()) {
            throw new InternalServerException("TOTP encryption key is not configured");
        }
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
            throw new InternalServerException("TOTP encryption key must decode to 16, 24, or 32 bytes");
        }
        return new SecretKeySpec(decoded, "AES");
    }
}
