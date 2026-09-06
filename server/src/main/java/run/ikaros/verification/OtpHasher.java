package run.ikaros.verification;

/**
 * 对验证码进行不可逆摘要与恒定时间校验的密码学端口。
 */
public interface OtpHasher {
    /**
     * 为验证码生成可持久化的慢哈希摘要。
     *
     * @param code 原始验证码
     * @return 可验证的摘要
     */
    String hash(String code);

    /**
     * 校验原始验证码是否匹配已保存摘要。
     *
     * @param code 原始验证码
     * @param digest 已保存摘要
     * @return 是否匹配
     */
    boolean matches(String code, String digest);
}
