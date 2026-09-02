package run.ikaros.verification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

/**
 * 使用随机盐与 PBKDF2 保存验证码摘要，避免数据库泄漏后直接读取验证码。
 */
@Component
public class Pbkdf2OtpHasher implements OtpHasher {
    /** PBKDF2 迭代次数。 */
    private static final int ITERATIONS = 120_000;
    /** 结果长度。 */
    private static final int KEY_LENGTH = 256;
    /** 安全随机源。 */
    private final SecureRandom random = new SecureRandom();

    @Override
    public String hash(String code) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(salt) + ":"
            + Base64.getUrlEncoder().withoutPadding().encodeToString(derive(code, salt));
    }

    @Override
    public boolean matches(String code, String digest) {
        String[] parts = digest.split(":", -1);
        if (parts.length != 2) {
            return false;
        }
        byte[] expected = Base64.getUrlDecoder().decode(parts[1]);
        byte[] actual = derive(code, Base64.getUrlDecoder().decode(parts[0]));
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] derive(String code, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(code.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException | java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("无法生成 OTP 摘要", exception);
        }
    }
}
