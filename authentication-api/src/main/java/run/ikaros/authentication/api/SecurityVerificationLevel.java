package run.ikaros.authentication.api;

/** 安全操作前的身份验证保证等级；权限本身不会因等级提升而增加。 */
public enum SecurityVerificationLevel {
    SVL_0(0),
    SVL_1(1),
    SVL_2(2),
    SVL_3(3),
    SVL_4(4);

    private final int value;

    SecurityVerificationLevel(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static SecurityVerificationLevel fromValue(int value) {
        for (SecurityVerificationLevel level : values()) {
            if (level.value == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("不支持的安全验证等级: " + value);
    }
}
