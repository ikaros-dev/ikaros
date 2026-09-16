package run.ikaros.authentication;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** 统一生成和校验平台密码凭据哈希。 */
final class PasswordHashService {
    private static final int ITERATIONS = 120000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHashService() {
    }

    static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        try {
            return "pbkdf2-sha256$" + ITERATIONS + "$" + encode(salt) + "$"
                + encode(derive(password.toCharArray(), salt));
        } catch (Exception exception) {
            throw new IllegalStateException("密码哈希失败", exception);
        }
    }

    static boolean matches(String password, String stored) {
        try {
            String[] parts = stored.split("\\$", 4);
            return parts.length == 4 && MessageDigest.isEqual(derive(password.toCharArray(),
                Base64.getUrlDecoder().decode(parts[2])), Base64.getUrlDecoder().decode(parts[3]));
        } catch (Exception exception) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt) throws Exception {
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)).getEncoded();
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
