package run.ikaros.common.api;

import java.util.UUID;

/** 生成平台内部实体使用的 UUIDv7 标识。 */
public interface UuidV7Generator {
    UUID next();
}
