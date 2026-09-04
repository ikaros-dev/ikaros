package run.ikaros.storage;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import run.ikaros.common.ConflictException;

@Component
public class StorageCredentialCipher {
    private final String encodedKey;
    private final SecureRandom random = new SecureRandom();

    public StorageCredentialCipher(@Value("${ikaros.storage.credential-encryption-key:}") String encodedKey) {
        this.encodedKey = encodedKey;
    }

    public String encrypt(String value) {
        if (value == null || value.isBlank()) return null;
        try { byte[] iv = new byte[12]; random.nextBytes(iv); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, iv)); byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)); return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(encrypted); }
        catch (ConflictException error) { throw error; }
        catch (Exception error) { throw new ConflictException("Storage credential 加密失败"); }
    }

    public String decrypt(String value) {
        if (value == null || value.isBlank()) return null;
        try { String[] parts = value.split(":", 2); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, Base64.getDecoder().decode(parts[0]))); return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8); }
        catch (Exception error) { throw new ConflictException("Storage credential 密文无法解密"); }
    }

    private byte[] key() {
        try { byte[] decoded = Base64.getDecoder().decode(encodedKey == null ? "" : encodedKey); if (decoded.length != 32) throw new IllegalArgumentException(); return decoded; }
        catch (IllegalArgumentException error) { throw new ConflictException("Storage credential encryption key 必须是 32 字节 Base64"); }
    }
}
