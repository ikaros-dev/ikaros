package run.ikaros.identity;

/**
 * 安全操作前的身份验证保证等级；权限本身不会因等级提升而增加。
 */
public enum SecurityVerificationLevel {
    SVL_0(0),
    SVL_1(1),
    SVL_2(2),
    SVL_3(3),
    SVL_4(4);

    /** 等级的数值排序依据。 */
    private final int value;

    SecurityVerificationLevel(int value) {
        this.value = value;
    }

    /**
     * 获取可用于策略比较的等级数值。
     *
     * @return 从零开始的等级数值
     */
    public int value() {
        return value;
    }

    /**
     * 将持久化数值转换为安全验证等级。
     *
     * @param value 数据库保存的等级数值
     * @return 对应的安全验证等级
     */
    public static SecurityVerificationLevel fromValue(int value) {
        for (SecurityVerificationLevel level : values()) {
            if (level.value == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("不支持的安全验证等级: " + value);
    }
}
