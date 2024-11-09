package run.ikaros.server.cache;

import reactor.core.publisher.Mono;

public interface ReactiveCacheManager {

    Mono<String> get(String key);

    Mono<Void> put(String key, String value);

    Mono<Void> remove(String key);
}
