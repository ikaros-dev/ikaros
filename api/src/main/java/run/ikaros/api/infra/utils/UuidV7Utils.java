package run.ikaros.api.infra.utils;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.annotation.Nullable;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    public static UUID generateUuid() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    /**
     * 从字符串转化成UUID对象，可以返回null.
     *
     * @param uuid uuid v7 字符串
     * @return 对象 或者 null
     */
    @Nullable
    public static UUID fromString(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            log.warn("Fail for convert uuid str to uuid obj. str:[{}]", uuid, e);
            return null;
        }
    }
}
