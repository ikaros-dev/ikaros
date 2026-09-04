package run.ikaros.storage;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import run.ikaros.common.ConflictException;

/** Envelope encryption for server-side provider credentials. */
@Component
public class StorageCredentialCipher {
    private static final String ENVELOPE_VERSION = "enc-v1";
    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final Map<String, byte[]> keys;
    private final String activeKeyVersion;
    private final SecureRandom random = new SecureRandom();

    public StorageCredentialCipher(
        @Value("${ikaros.storage.credential-encryption-key:}") String activeKey,
        @Value("${ikaros.storage.credential-encryption-keys:}") String configuredKeys,
        @Value("${ikaros.storage.credential-encryption-key-version:v1}") String activeKeyVersion) {
        this.keys = parseKeys(activeKey, configuredKeys, activeKeyVersion);
        this.activeKeyVersion = activeKeyVersion == null || activeKeyVersion.isBlank() ? "v1" : activeKeyVersion.trim();
    }

    /** Test and non-Spring compatibility constructor for a single active key. */
    public StorageCredentialCipher(String activeKey) { this(activeKey, "", "v1"); }

    public String encrypt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            byte[] dek = randomBytes(KEY_LENGTH);
            byte[] dataIv = randomBytes(IV_LENGTH);
            byte[] wrappedIv = randomBytes(IV_LENGTH);
            byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, dek, dataIv, value.getBytes(StandardCharsets.UTF_8));
            byte[] wrapped = crypt(Cipher.ENCRYPT_MODE, keyFor(activeKeyVersion), wrappedIv, dek);
            return String.join(":", ENVELOPE_VERSION, activeKeyVersion, encode(wrappedIv), encode(wrapped),
                encode(dataIv), encode(encrypted));
        } catch (GeneralSecurityException error) {
            throw new ConflictException("Storage credential 加密失败");
        }
    }

    public String decrypt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String[] parts = value.split(":", -1);
            if (parts.length == 6 && ENVELOPE_VERSION.equals(parts[0])) {
                byte[] dek = crypt(Cipher.DECRYPT_MODE, keyFor(parts[1]), decode(parts[2]), decode(parts[3]));
                return new String(crypt(Cipher.DECRYPT_MODE, dek, decode(parts[4]), decode(parts[5])), StandardCharsets.UTF_8);
            }
            if (parts.length == 2) {
                return new String(crypt(Cipher.DECRYPT_MODE, keyFor(activeKeyVersion), decode(parts[0]), decode(parts[1])), StandardCharsets.UTF_8);
            }
            throw new GeneralSecurityException("unsupported credential envelope");
        } catch (ConflictException error) {
            throw error;
        } catch (Exception error) {
            throw new ConflictException("Storage credential 密文无法解密");
        }
    }

    public boolean needsReEncryption(String value) {
        if (value == null || value.isBlank()) return false;
        String[] parts = value.split(":", -1);
        return parts.length != 6 || !ENVELOPE_VERSION.equals(parts[0]) || !activeKeyVersion.equals(parts[1]);
    }

    public String reEncrypt(String value) {
        return value == null || value.isBlank() || !needsReEncryption(value) ? value : encrypt(decrypt(value));
    }

    public String activeKeyVersion() { return activeKeyVersion; }

    private byte[] crypt(int mode, byte[] key, byte[] iv, byte[] input) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
        return cipher.doFinal(input);
    }

    private byte[] keyFor(String version) {
        if (keys.isEmpty()) throw invalidKey();
        byte[] key = keys.get(version);
        if (key == null) throw new ConflictException("Storage credential 主密钥版本不存在: " + version);
        return key;
    }

    private Map<String, byte[]> parseKeys(String activeKey, String configuredKeys, String activeVersion) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        if (configuredKeys != null && !configuredKeys.isBlank()) {
            for (String entry : configuredKeys.split(",")) {
                String[] pair = entry.trim().split("=", 2);
                if (pair.length != 2 || pair[0].isBlank()) throw invalidKey();
                result.put(pair[0].trim(), decodeKey(pair[1].trim()));
            }
        }
        if (activeKey != null && !activeKey.isBlank()) {
            result.put(activeVersion == null || activeVersion.isBlank() ? "v1" : activeVersion.trim(), decodeKey(activeKey.trim()));
        }
        return result;
    }

    private byte[] decodeKey(String encoded) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            if (decoded.length != KEY_LENGTH) throw new IllegalArgumentException();
            return decoded;
        } catch (IllegalArgumentException error) {
            throw invalidKey();
        }
    }

    private ConflictException invalidKey() { return new ConflictException("Storage credential encryption key 必须是 32 字节 Base64"); }
    private byte[] randomBytes(int length) { byte[] value = new byte[length]; random.nextBytes(value); return value; }
    private String encode(byte[] value) { return Base64.getEncoder().encodeToString(value); }
    private byte[] decode(String value) { return Base64.getDecoder().decode(value); }
}
