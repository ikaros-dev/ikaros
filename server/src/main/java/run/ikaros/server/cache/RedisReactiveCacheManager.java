package run.ikaros.server.cache;

import reactor.core.publisher.Mono;

public class RedisReactiveCacheManager implements ReactiveCacheManager {


    @Override
    public Mono<String> get(String key) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> put(String key, String value) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> remove(String key) {
        return Mono.empty();
    }

}
