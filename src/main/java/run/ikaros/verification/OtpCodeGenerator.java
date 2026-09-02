package run.ikaros.verification;

/**
 * 生成不应被持久化的一次性验证码。
 */
public interface OtpCodeGenerator {
    /**
     * 生成六位数字验证码。
     *
     * @return 仅可短暂传递给投递 Provider 的验证码
     */
    String generate();
}
