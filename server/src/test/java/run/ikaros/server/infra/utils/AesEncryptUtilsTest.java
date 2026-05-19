package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AesEncryptUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void generateKeyByteArrayDefaultLength() {
        byte[] keyBytes = AesEncryptUtils.generateKeyByteArray();
        assertNotNull(keyBytes);
        assertTrue(keyBytes.length > 0, "Default key bytes should not be empty");
        // Default is 192-bit, Base64-encoded: ceil(192/8) = 24 raw bytes -> Base64 = 32 chars
        byte[] decoded = Base64.getDecoder().decode(keyBytes);
        assertEquals(24, decoded.length, "Default 192-bit key should decode to 24 bytes");
    }

    @Test
    void generateKeyByteArray128() {
        byte[] keyBytes = AesEncryptUtils.generateKeyByteArray(128);
        assertNotNull(keyBytes);
        byte[] decoded = Base64.getDecoder().decode(keyBytes);
        assertEquals(16, decoded.length, "128-bit key should decode to 16 bytes");
    }

    @Test
    void generateKeyByteArray256() {
        byte[] keyBytes = AesEncryptUtils.generateKeyByteArray(256);
        assertNotNull(keyBytes);
        byte[] decoded = Base64.getDecoder().decode(keyBytes);
        assertEquals(32, decoded.length, "256-bit key should decode to 32 bytes");
    }

    @Test
    void encryptDecryptByteArrayRoundtrip() {
        byte[] keyBytes = Base64.getDecoder().decode(AesEncryptUtils.generateKeyByteArray());
        byte[] original = "Hello, AES encryption!".getBytes();

        byte[] encrypted = AesEncryptUtils.encryptByteArray(keyBytes, original);
        assertNotNull(encrypted);
        byte[] decrypted = AesEncryptUtils.decryptByteArray(keyBytes, encrypted);
        assertNotNull(decrypted);
        assertArrayEquals(original, decrypted);
    }

    @Test
    void encryptDecryptWithBase64StringKey() {
        byte[] keyBytes = AesEncryptUtils.generateKeyByteArray();
        String keyStrBase64 = new String(keyBytes);
        byte[] original = "Test string key encryption".getBytes();

        byte[] encrypted = AesEncryptUtils.encryptByteArray(keyStrBase64, original);
        assertNotNull(encrypted);
        byte[] decrypted = AesEncryptUtils.decryptByteArray(keyStrBase64, encrypted);
        assertNotNull(decrypted);
        assertArrayEquals(original, decrypted);
    }

    @Test
    void generateKeyFileAndEncryptDecrypt() throws IOException {
        File keyFile = tempDir.resolve("aes.key").toFile();
        AesEncryptUtils.generateKeyFile(keyFile);
        assertTrue(keyFile.exists());
        assertTrue(keyFile.length() > 0);

        byte[] keyBytes = Base64.getDecoder().decode(Files.readAllBytes(keyFile.toPath()));
        byte[] original = "File key test data".getBytes();

        byte[] encrypted = AesEncryptUtils.encryptByteArray(keyFile, original);
        assertNotNull(encrypted);
        byte[] decrypted = AesEncryptUtils.decryptByteArray(keyBytes, encrypted);
        assertArrayEquals(original, decrypted);
    }

    @Test
    void encryptDecryptInputStreamRoundtrip() throws IOException {
        byte[] keyBytes = Base64.getDecoder().decode(AesEncryptUtils.generateKeyByteArray());
        byte[] original = "Stream encryption test content".getBytes();

        // Encrypt
        ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();
        AesEncryptUtils.encryptInputStream(
            new ByteArrayInputStream(original), true, encryptedOut, keyBytes);
        byte[] encrypted = encryptedOut.toByteArray();

        // Decrypt
        ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream();
        AesEncryptUtils.decryptInputStream(
            new ByteArrayInputStream(encrypted), true, decryptedOut, keyBytes);
        byte[] decrypted = decryptedOut.toByteArray();

        assertArrayEquals(original, decrypted);
    }

    @Test
    void encryptDecryptFileWithFileKey() throws IOException {
        File keyFile = tempDir.resolve("key.dat").toFile();
        AesEncryptUtils.generateKeyFile(keyFile);

        File dataFile = tempDir.resolve("data.txt").toFile();
        byte[] original = "File encryption roundtrip".getBytes();
        try (FileOutputStream fos = new FileOutputStream(dataFile)) {
            fos.write(original);
        }

        File encryptedFile = tempDir.resolve("encrypted.dat").toFile();
        AesEncryptUtils.encryptFile(keyFile, dataFile, encryptedFile);
        assertTrue(encryptedFile.exists());

        File decryptedFile = tempDir.resolve("decrypted.txt").toFile();
        AesEncryptUtils.decryptFile(keyFile, encryptedFile, decryptedFile);
        assertTrue(decryptedFile.exists());

        byte[] decrypted = Files.readAllBytes(decryptedFile.toPath());
        assertArrayEquals(original, decrypted);
    }

    @Test
    void encryptFileWithByteArrayKeyAndDecrypt() throws IOException {
        byte[] keyBytes = Base64.getDecoder().decode(AesEncryptUtils.generateKeyByteArray());

        File dataFile = tempDir.resolve("input.txt").toFile();
        byte[] original = "ByteArray key file test".getBytes();
        try (FileOutputStream fos = new FileOutputStream(dataFile)) {
            fos.write(original);
        }

        byte[] encrypted = AesEncryptUtils.encryptFile(keyBytes, dataFile);
        assertNotNull(encrypted);

        byte[] decrypted = AesEncryptUtils.decryptFile(keyBytes,
            createTempFileFromBytes("encrypted.dat", encrypted));
        assertArrayEquals(original, decrypted);
    }

    @Test
    void encryptEmptyByteArray() {
        byte[] keyBytes = Base64.getDecoder().decode(AesEncryptUtils.generateKeyByteArray());
        byte[] original = new byte[0];

        byte[] encrypted = AesEncryptUtils.encryptByteArray(keyBytes, original);
        assertNotNull(encrypted);

        byte[] decrypted = AesEncryptUtils.decryptByteArray(keyBytes, encrypted);
        assertNotNull(decrypted);
        assertArrayEquals(original, decrypted);
    }

    private File createTempFileFromBytes(String name, byte[] bytes) throws IOException {
        File file = tempDir.resolve(name).toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
        }
        return file;
    }
}
