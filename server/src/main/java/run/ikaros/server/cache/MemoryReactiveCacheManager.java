package run.ikaros.server.cache;

import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Mono;

public class MemoryReactiveCacheManager implements ReactiveCacheManager {
    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();


    @Override
    public Mono<String> get(String key) {
        return Mono.just(CACHE.get(key));
    }

    @Override
    public Mono<Void> put(String key, String value) {
        CACHE.put(key, value);
        return Mono.empty();
    }

    @Override
    public Mono<Void> remove(String key) {
        CACHE.remove(key);
        return Mono.empty();
    }

}
