package run.ikaros.server.cache;

import reactor.core.publisher.Mono;

public interface ReactiveCacheManager {

    Mono<Boolean> containsKey(String key);

    Mono<Object> get(String key);

    Mono<Boolean> put(String key, Object value);

    Mono<Boolean> remove(String key);

    Mono<String> clear();
}
