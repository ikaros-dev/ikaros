package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AesEncryptUtilsTest {

    @Test
    void generateKeyByteArray_default_returnsBase64Bytes() {
        byte[] keyB64 = AesEncryptUtils.generateKeyByteArray();
        assertThat(keyB64).isNotNull();
        // decode to verify it's valid base64 and 24 raw bytes (192-bit default)
        byte[] rawKey = Base64
            .getDecoder()
            .decode(keyB64);
        assertThat(rawKey).hasSize(24);
    }

    @Test
    void generateKeyByteArray_128bit_returnsValidKey() {
        byte[] keyB64 = AesEncryptUtils.generateKeyByteArray(128);
        assertThat(keyB64).isNotNull();
        byte[] rawKey = Base64
            .getDecoder()
            .decode(keyB64);
        assertThat(rawKey).hasSize(16); // 128 bits = 16 bytes
    }

    @Test
    void generateKeyByteArray_256bit_returnsValidKey() {
        byte[] keyB64 = AesEncryptUtils.generateKeyByteArray(256);
        assertThat(keyB64).isNotNull();
        byte[] rawKey = Base64
            .getDecoder()
            .decode(keyB64);
        assertThat(rawKey).hasSize(32); // 256 bits = 32 bytes
    }

    @Test
    void encryptDecrypt_roundTrip_bytes() throws Exception {
        byte[] keyB64 = AesEncryptUtils.generateKeyByteArray(128);
        byte[] rawKey = Base64
            .getDecoder()
            .decode(keyB64);
        String original = "Hello AES World! " + System.currentTimeMillis();
        byte[] originalBytes = original.getBytes(StandardCharsets.UTF_8);

        // encryptByteArray(byte[] key, byte[] data)
        byte[] encrypted = AesEncryptUtils.encryptByteArray(rawKey, originalBytes);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(originalBytes);

        // decryptByteArray(byte[] key, byte[] encryptedDataBase64)
        byte[] decrypted = AesEncryptUtils.decryptByteArray(rawKey, encrypted);
        String result = new String(decrypted, StandardCharsets.UTF_8);
        assertThat(result).isEqualTo(original);
    }

    @Test
    void encryptDecrypt_withStringKey() throws Exception {
        byte[] keyB64 = AesEncryptUtils.generateKeyByteArray(128);
        String keyBase64Str = new String(keyB64, StandardCharsets.UTF_8);
        String original = "测试中文加密内容";

        // encryptByteArray(String base64Key, byte[] data)
        byte[] encrypted = AesEncryptUtils.encryptByteArray(keyBase64Str,
            original.getBytes(StandardCharsets.UTF_8));
        assertThat(encrypted).isNotNull();

        byte[] rawKey = Base64
            .getDecoder()
            .decode(keyB64);
        byte[] decrypted = AesEncryptUtils.decryptByteArray(rawKey, encrypted);
        String result = new String(decrypted, StandardCharsets.UTF_8);
        assertThat(result).isEqualTo(original);
    }

    @Test
    void generateKeyFile_validPath_createsFile(@TempDir File tempDir) throws Exception {
        File keyFile = new File(tempDir, "aes.key");
        AesEncryptUtils.generateKeyFile(keyFile);
        assertThat(keyFile).exists();
        assertThat(keyFile.length()).isGreaterThan(0);
    }

    @Test
    void generateKeyFile_customLength_createsFile(@TempDir File tempDir) throws Exception {
        File keyFile = new File(tempDir, "aes256.key");
        AesEncryptUtils.generateKeyFile(256, keyFile);
        assertThat(keyFile).exists();
        // key file content is base64 encoded, should be ~44 chars for 256-bit = 32 raw bytes
        String content = Files
            .readString(keyFile.toPath())
            .trim();
        byte[] rawKey = Base64
            .getDecoder()
            .decode(content);
        assertThat(rawKey).hasSize(32);
    }

    @Test
    void encryptFile_decryptFile_roundTrip(@TempDir File tempDir) throws Exception {
        byte[] keyB64 = AesEncryptUtils.generateKeyByteArray(128);
        File keyFile = new File(tempDir, "secret.key");
        Files.write(keyFile.toPath(), keyB64);

        File originalFile = new File(tempDir, "original.txt");
        File encryptedFile = new File(tempDir, "encrypted.bin");
        File decryptedFile = new File(tempDir, "decrypted.txt");

        Files.write(originalFile.toPath(), "File encryption test".getBytes());

        // encryptFile(keyFile, dataFile, encryptedFile)
        AesEncryptUtils.encryptFile(keyFile, originalFile, encryptedFile);
        // decryptFile(keyFile, dataFile, decryptedFile)
        AesEncryptUtils.decryptFile(keyFile, encryptedFile, decryptedFile);

        String decryptedContent = Files.readString(decryptedFile.toPath());
        assertThat(decryptedContent).isEqualTo("File encryption test");
    }

    @Test
    void encryptByteArray_withKeyFile_roundTrip(@TempDir File tempDir) throws Exception {
        File keyFile = new File(tempDir, "secret.key");
        AesEncryptUtils.generateKeyFile(keyFile);
        String original = "Encrypt with key file";

        byte[] encrypted =
            AesEncryptUtils.encryptByteArray(keyFile, original.getBytes(StandardCharsets.UTF_8));
        assertThat(encrypted).isNotNull();

        byte[] keyB64 = Files.readAllBytes(keyFile.toPath());
        byte[] rawKey = Base64
            .getDecoder()
            .decode(keyB64);
        byte[] decrypted = AesEncryptUtils.decryptByteArray(rawKey, encrypted);
        String result = new String(decrypted, StandardCharsets.UTF_8);
        assertThat(result).isEqualTo(original);
    }
}
