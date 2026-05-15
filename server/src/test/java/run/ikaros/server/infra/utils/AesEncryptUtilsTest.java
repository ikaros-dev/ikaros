package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AesEncryptUtilsTest {

    @TempDir
    File tempDir;

    @Test
    void generateKeyByteArray_defaultLength() {
        byte[] key = AesEncryptUtils.generateKeyByteArray();
        assertNotNull(key);
        assertTrue(key.length > 0);
    }

    @Test
    void generateKeyByteArray_specificLength() {
        byte[] key128 = AesEncryptUtils.generateKeyByteArray(128);
        byte[] key192 = AesEncryptUtils.generateKeyByteArray(192);
        byte[] key256 = AesEncryptUtils.generateKeyByteArray(256);
        assertNotNull(key128);
        assertNotNull(key192);
        assertNotNull(key256);
        assertTrue(key128.length > 0);
        assertTrue(key192.length > 0);
        assertTrue(key256.length > 0);
    }

    @Test
    void generateKeyFile_defaultLength() throws IOException {
        File keyFile = new File(tempDir, "key.dat");
        AesEncryptUtils.generateKeyFile(keyFile);
        assertTrue(keyFile.exists());
        assertTrue(keyFile.length() > 0);
    }

    @Test
    void encryptDecryptByteArray_roundTrip() {
        byte[] key = AesEncryptUtils.generateKeyByteArray();
        byte[] original = "Hello, World! This is a test message.".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = AesEncryptUtils.encryptByteArray(key, original);
        assertNotNull(encrypted);

        byte[] decrypted = AesEncryptUtils.decryptByteArray(key, encrypted);
        assertArrayEquals(original, decrypted);
    }

    @Test
    void encryptDecryptByteArray_withStringKey() {
        byte[] key = AesEncryptUtils.generateKeyByteArray();
        String keyBase64 = new String(key, StandardCharsets.UTF_8);
        byte[] original = "Test message".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = AesEncryptUtils.encryptByteArray(keyBase64, original);
        byte[] decrypted = AesEncryptUtils.decryptByteArray(keyBase64, encrypted);
        assertArrayEquals(original, decrypted);
    }

    @Test
    void encryptByteArray_withKeyFile() throws IOException {
        File keyFile = new File(tempDir, "key.dat");
        AesEncryptUtils.generateKeyFile(keyFile);

        byte[] original = "File-based key test".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = AesEncryptUtils.encryptByteArray(keyFile, original);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 0);
    }

    @Test
    void encryptDecryptInputStream_roundTrip() throws IOException {
        byte[] key = AesEncryptUtils.generateKeyByteArray();
        byte[] original = "Stream encryption test".getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();
        AesEncryptUtils.encryptInputStream(
            new ByteArrayInputStream(original), true, encryptedOut, key);
        byte[] encrypted = encryptedOut.toByteArray();

        ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream();
        AesEncryptUtils.decryptInputStream(
            new ByteArrayInputStream(encrypted), true, decryptedOut, key);
        byte[] decrypted = decryptedOut.toByteArray();

        assertArrayEquals(original, decrypted);
    }

    @Test
    void encryptDecryptFile_roundTrip() throws IOException {
        File keyFile = new File(tempDir, "key.dat");
        AesEncryptUtils.generateKeyFile(keyFile);

        File dataFile = new File(tempDir, "data.txt");
        Files.write(dataFile.toPath(), "File encryption test".getBytes(StandardCharsets.UTF_8));

        File encryptedFile = new File(tempDir, "encrypted.dat");
        AesEncryptUtils.encryptFile(keyFile, dataFile, encryptedFile);
        assertTrue(encryptedFile.exists());

        File decryptedFile = new File(tempDir, "decrypted.txt");
        AesEncryptUtils.decryptFile(keyFile, encryptedFile, decryptedFile);
        assertTrue(decryptedFile.exists());

        String decryptedContent = new String(Files.readAllBytes(decryptedFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(decryptedContent.contains("File encryption test"));
    }

    @Test
    void encryptFile_withKeyFile() throws IOException {
        File keyFile = new File(tempDir, "key.dat");
        AesEncryptUtils.generateKeyFile(keyFile);

        File dataFile = new File(tempDir, "data.txt");
        Files.write(dataFile.toPath(), "Data to encrypt".getBytes(StandardCharsets.UTF_8));

        byte[] encrypted = AesEncryptUtils.encryptFile(keyFile, dataFile);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 0);
    }
}
