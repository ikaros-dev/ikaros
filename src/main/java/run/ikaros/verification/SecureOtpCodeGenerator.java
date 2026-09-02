package run.ikaros.verification;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * 使用密码学安全随机数生成固定长度验证码。
 */
@Component
public class SecureOtpCodeGenerator implements OtpCodeGenerator {
    /** 安全随机源。 */
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        return "%06d".formatted(random.nextInt(1_000_000));
    }
}
