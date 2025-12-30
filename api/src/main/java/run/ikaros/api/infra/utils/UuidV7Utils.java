package run.ikaros.api.infra.utils;

import com.github.f4b6a3.uuid.UuidCreator;

public class UuidV7Utils {
    /**
     * 生成一个 UUIDv7 字符串.
     */
    public static String generate() {
        return UuidCreator.getTimeOrderedEpoch().toString();
    }

    /**
     * 直接生成 UUID 对象，便于存储或进一步处理.
     */
    public static java.util.UUID generateUuid() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
