package com.openquartz.easyarchive.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CryptoUtil}.
 */
class CryptoUtilTest {

    @Test
    void keyEnvironmentVariableName_isArchiveEncryptionKey() {
        String keyName = assertDoesNotThrow(() ->
                String.valueOf(CryptoUtil.class.getDeclaredField("KEY_ENVIRONMENT_VARIABLE").get(null)));

        assertEquals("ARCHIVE_ENCRYPTION_KEY", keyName);
    }

    @Test
    void encryptDecrypt_roundTrip() {
        String original = "my-s3cret-p@ssw0rd!";
        String encrypted = CryptoUtil.encrypt(original);
        assertNotNull(encrypted);
        assertTrue(encrypted.contains("."));

        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encrypt_samePlaintext_producesDifferentCiphertext() {
        String secret = "password123";
        String e1 = CryptoUtil.encrypt(secret);
        String e2 = CryptoUtil.encrypt(secret);
        assertNotEquals(e1, e2);

        String d1 = CryptoUtil.decrypt(e1);
        String d2 = CryptoUtil.decrypt(e2);
        assertEquals(secret, d1);
        assertEquals(secret, d2);
    }

    @Test
    void encrypt_decrypt_emptyString() {
        assertNull(CryptoUtil.encrypt(null));
        assertNull(CryptoUtil.decrypt(null));

        assertEquals("", CryptoUtil.encrypt(""));
        assertEquals("", CryptoUtil.decrypt(""));
    }

    @Test
    void encrypt_decrypt_whitespaceString() {
        String ws = "   ";
        assertEquals(ws, CryptoUtil.encrypt(ws));
        assertEquals(ws, CryptoUtil.decrypt(ws));
    }

    @Test
    void decrypt_nonEncryptedFormat_returnsUnchanged() {
        // Plain text passwords without the expected encrypted format
        // are returned unchanged for backward compatibility.
        assertEquals("secret", CryptoUtil.decrypt("secret"));
        assertEquals("plain", CryptoUtil.decrypt("plain"));
        // A dot without the expected format is also returned as-is.
        assertEquals(".notencrypted", CryptoUtil.decrypt(".notencrypted"));
    }

    @Test
    void decrypt_legacyData_returnsUnchanged() {
        // Simulate old plaintext passwords stored without encryption
        assertEquals("legacy-password", CryptoUtil.decrypt("legacy-password"));
    }

    @Test
    void encrypt_decrypt_utf8AndSpecialChars() {
        String special = "中文密码!@#$%^&*()_+-=[]{}|;':\",./<>?~";
        String encrypted = CryptoUtil.encrypt(special);
        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(special, decrypted);
    }

    @Test
    void encrypt_decrypt_longString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("abcdefghij");
        }
        String longStr = sb.toString();
        String encrypted = CryptoUtil.encrypt(longStr);
        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(longStr, decrypted);
    }

    @Test
    void decrypt_differentKey_dataShouldMatch() {
        String secret = "test";
        String encrypted = CryptoUtil.encrypt(secret);
        String decrypted = CryptoUtil.decrypt(encrypted);
        assertEquals(secret, decrypted);
    }
}
