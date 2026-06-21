package com.openquartz.easyarchive.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for datasource password encryption.
 * <p>
 * Algorithm: AES-256-GCM with 12-byte random IV and 128-bit auth tag.
 * Ciphertext format: base64(iv).base64(ciphertext + authTag)
 * Key source: Environment variable EASY_ARCHIVE_AES_KEY (padded/truncated to 32 bytes).
 *
 * @author svnee
 */
public final class CryptoUtil {

    public static final String KEY_ENVIRONMENT_VARIABLE = "ARCHIVE_ENCRYPTION_KEY";

    private static final String AES = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_BYTE_LENGTH = 32;
    private static final char SEPARATOR = '.';

    private static final SecretKeySpec KEY;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        String raw = System.getenv(KEY_ENVIRONMENT_VARIABLE);
        byte[] b;
        if (StringUtils.isBlank(raw)) {
            // Dev-only fallback. MUST NOT be used in production.
            raw = "easy-archive-dev-key!!01";
        }
        b = raw.getBytes(StandardCharsets.UTF_8);
        if (b.length > AES_KEY_BYTE_LENGTH) {
            b = Arrays.copyOf(b, AES_KEY_BYTE_LENGTH);
        } else if (b.length < AES_KEY_BYTE_LENGTH) {
            b = Arrays.copyOf(b, AES_KEY_BYTE_LENGTH);
        }
        KEY = new SecretKeySpec(b, AES);
    }

    private CryptoUtil() {
        // utility class
    }

    /**
     * Encrypt a plaintext string.
     *
     * @param plaintext the string to encrypt
     * @return base64 ciphertext in format: iv.cipherWithTag
     */
    public static String encrypt(String plaintext) {
        if (StringUtils.isBlank(plaintext)) {
            return plaintext;
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, KEY, spec);

            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return base64NoPad(iv) + SEPARATOR + base64NoPad(ct);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
        }
    }

    /**
     * Decrypt a base64-encoded ciphertext.
     * <p>
     * Returns the original value unchanged if it does not appear to be
     * an encrypted value (missing delimiter or malformed base64),
     * providing safe no-op fallback for legacy plaintext passwords.
     *
     * @param ciphertext ciphertext produced by {@link #encrypt(String)}
     * @return decrypted plaintext, or the original value if not encrypted
     */
    public static String decrypt(String ciphertext) {
        if (StringUtils.isBlank(ciphertext)) {
            return ciphertext;
        }

        try {
            int dot = ciphertext.lastIndexOf(SEPARATOR);
            if (dot <= 0 || dot == ciphertext.length() - 1) {
                // Not in the expected encrypted format — return as-is.
                // This safely handles legacy plaintext passwords.
                return ciphertext;
            }

            byte[] iv = base64Decode(ciphertext.substring(0, dot));
            byte[] ct = base64Decode(ciphertext.substring(dot + 1));

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, KEY, spec);

            byte[] plain = cipher.doFinal(ct);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Malformed ciphertext — return as-is for backward compatibility.
            return ciphertext;
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decryption failed", e);
        }
    }

    /** Base64-encode, stripping trailing padding '=' characters. */
    private static String base64NoPad(byte[] input) {
        return Base64.getEncoder().encodeToString(input).replaceAll("=+$", "");
    }

    /** Base64-decode, restoring padding as needed. */
    private static byte[] base64Decode(String encoded) {
        int pad = (4 - encoded.length() % 4) % 4;
        encoded += "=".repeat(pad);
        return Base64.getDecoder().decode(encoded);
    }
}
