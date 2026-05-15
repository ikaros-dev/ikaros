package run.ikaros.api.infra.utils;

import java.util.UUID;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ReactiveUuidV7Utils {
    /**
     * 生成一个 UUIDv7 字符串.
     */
    public static Mono<String> generate() {
        return Mono.fromCallable(UuidV7Utils::generate)
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 直接生成 UUID 对象，便于存储或进一步处理.
     */
    public static Mono<UUID> generateUuid() {
        return Mono.fromCallable(UuidV7Utils::generateUuid)
            .subscribeOn(Schedulers.boundedElastic());
    }
}
