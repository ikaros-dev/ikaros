package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void generateKeyFile_withSpecificLength() throws IOException {
        File keyFile = new File(tempDir, "key128.dat");
        AesEncryptUtils.generateKeyFile(128, keyFile);
        assertTrue(keyFile.exists());
        assertTrue(keyFile.length() > 0);
    }

    @Test
    void generateKeyFile_createsParentDir() throws IOException {
        File keyFile = new File(tempDir, "subdir/key.dat");
        AesEncryptUtils.generateKeyFile(keyFile);
        assertTrue(keyFile.exists());
        assertTrue(keyFile.getParentFile().exists());
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
    void encryptDecryptByteArray_emptyData() {
        byte[] key = AesEncryptUtils.generateKeyByteArray();
        byte[] original = new byte[0];

        byte[] encrypted = AesEncryptUtils.encryptByteArray(key, original);
        assertNotNull(encrypted);

        byte[] decrypted = AesEncryptUtils.decryptByteArray(key, encrypted);
        assertArrayEquals(original, decrypted);
    }

    @Test
    void encryptDecryptByteArray_largeData() {
        byte[] key = AesEncryptUtils.generateKeyByteArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("This is a test line number ").append(i).append("\n");
        }
        byte[] original = sb.toString().getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = AesEncryptUtils.encryptByteArray(key, original);
        assertNotNull(encrypted);

        byte[] decrypted = AesEncryptUtils.decryptByteArray(key, encrypted);
        assertArrayEquals(original, decrypted);
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
    void encryptInputStream_notCloseInputStream() throws IOException {
        byte[] key = AesEncryptUtils.generateKeyByteArray();
        byte[] original = "Test".getBytes(StandardCharsets.UTF_8);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(original);
        ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();

        AesEncryptUtils.encryptInputStream(inputStream, false, encryptedOut, key);

        // Stream should still be open, we can read from it
        assertTrue(inputStream.read() == -1); // Data was consumed
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

    @Test
    void encryptFile_withKeyBytes() throws IOException {
        byte[] key = AesEncryptUtils.generateKeyByteArray();

        File dataFile = new File(tempDir, "data.txt");
        Files.write(dataFile.toPath(), "Data to encrypt".getBytes(StandardCharsets.UTF_8));

        byte[] encrypted = AesEncryptUtils.encryptFile(key, dataFile);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 0);
    }

    @Test
    void decryptFile_withKeyBytes() throws IOException {
        byte[] key = AesEncryptUtils.generateKeyByteArray();

        File dataFile = new File(tempDir, "data.txt");
        Files.write(dataFile.toPath(), "Data to encrypt".getBytes(StandardCharsets.UTF_8));

        // Encrypt and get bytes
        byte[] encryptedBytes = AesEncryptUtils.encryptFile(key, dataFile);

        // Write encrypted bytes to file
        File encryptedFile = new File(tempDir, "encrypted.dat");
        Files.write(encryptedFile.toPath(), encryptedBytes);

        // Decrypt
        byte[] decrypted = AesEncryptUtils.decryptFile(key, encryptedFile);
        assertNotNull(decrypted);
        assertTrue(decrypted.length > 0);
        assertTrue(new String(decrypted, StandardCharsets.UTF_8).contains("Data to encrypt"));
    }

    @Test
    void decryptFile_withKeyFile() throws IOException {
        File keyFile = new File(tempDir, "key.dat");
        AesEncryptUtils.generateKeyFile(keyFile);

        File dataFile = new File(tempDir, "data.txt");
        Files.write(dataFile.toPath(), "Test data".getBytes(StandardCharsets.UTF_8));

        // Encrypt and get bytes
        byte[] encryptedBytes = AesEncryptUtils.encryptFile(keyFile, dataFile);

        // Write encrypted bytes to file
        File encryptedFile = new File(tempDir, "encrypted.dat");
        Files.write(encryptedFile.toPath(), encryptedBytes);

        // Decrypt
        byte[] decrypted = AesEncryptUtils.decryptFile(keyFile, encryptedFile);
        assertNotNull(decrypted);
        assertTrue(decrypted.length > 0);
        assertTrue(new String(decrypted, StandardCharsets.UTF_8).contains("Test data"));
    }

    @Test
    void decryptFile_createsOutputDir() throws IOException {
        File keyFile = new File(tempDir, "key.dat");
        AesEncryptUtils.generateKeyFile(keyFile);

        File dataFile = new File(tempDir, "data.txt");
        Files.write(dataFile.toPath(), "Test".getBytes(StandardCharsets.UTF_8));

        File encryptedFile = new File(tempDir, "encrypted.dat");
        AesEncryptUtils.encryptFile(keyFile, dataFile, encryptedFile);

        File decryptedFile = new File(tempDir, "subdir/decrypted.txt");
        AesEncryptUtils.decryptFile(keyFile, encryptedFile, decryptedFile);
        assertTrue(decryptedFile.exists());
        assertTrue(decryptedFile.getParentFile().exists());
    }
}
