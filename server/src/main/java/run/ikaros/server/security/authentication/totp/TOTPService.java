package run.ikaros.server.security.authentication.totp;

import jakarta.validation.constraints.NotBlank;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * TOTP (Time-based One-Time Password) 服务.
 * 实现RFC 6238标准，SHA1算法，30秒步长，6位验证码.
 */
@Slf4j
@Service
public class TotpService {

    private static final int SECRET_SIZE = 20; // 160 bits
    private static final int CODE_DIGITS = 6;
    private static final long TIME_STEP = 30; // 30 seconds
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成新的TOTP密钥(Base32编码).
     */
    public String generateSecret() {
        byte[] buffer = new byte[SECRET_SIZE];
        secureRandom.nextBytes(buffer);
        Base32 base32 = new Base32();
        return base32.encodeToString(buffer).replaceAll("=", "");
    }

    /**
     * 生成otpauth URI，用于导入Authenticator.
     *
     * @param username 用户名
     * @param secret   Base32密钥
     * @return otpauth://totp/ URI
     */
    public String generateOtpAuthUri(@NotBlank String username, @NotBlank String secret) {
        Assert.hasText(username, "'username' must has text.");
        Assert.hasText(secret, "'secret' must has text.");
        // 应用名称可以配置化，这里写死为Ikaros
        return "otpauth://totp/Ikaros:" + username
            + "?secret=" + secret
            + "&issuer=Ikaros"
            + "&algorithm=SHA1"
            + "&digits=" + CODE_DIGITS
            + "&period=" + TIME_STEP;
    }

    /**
     * 验证TOTP验证码.
     *
     * @param secret Base32密钥
     * @param code   用户输入的6位验证码
     * @return 是否有效（支持前后各1个时间窗口，共3个窗口）
     */
    public boolean validateCode(@NotBlank String secret, @NotBlank String code) {
        Assert.hasText(secret, "'secret' must has text.");
        Assert.hasText(code, "'code' must has text.");
        if (code.length() != CODE_DIGITS || !code.matches("\\d{" + CODE_DIGITS + "}")) {
            return false;
        }

        long currentTime = System.currentTimeMillis() / 1000L;
        long currentCounter = currentTime / TIME_STEP;

        // 检查当前窗口和前一个窗口（共3个窗口，容忍1步偏差）
        for (int i = -1; i <= 1; i++) {
            long counter = currentCounter + i;
            String expected = generateTotp(secret, counter);
            if (expected.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成指定计数器的TOTP验证码.
     */
    private String generateTotp(String secret, long counter) {
        try {
            Base32 base32 = new Base32();
            byte[] keyBytes = base32.decode(secret);

            // 将计数器转为8字节大端序
            byte[] counterBytes = new byte[8];
            for (int i = 7; i >= 0; i--) {
                counterBytes[i] = (byte) (counter & 0xff);
                counter >>= 8;
            }

            // HMAC-SHA1
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(counterBytes);

            // 动态截断
            int offset = hash[hash.length - 1] & 0xf;
            int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);

            // 取模生成6位数字
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TOTP", e);
        }
    }
}
