package run.ikaros.server.cache;

import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Mono;

public class MemoryReactiveCacheManager implements ReactiveCacheManager {
    private final ConcurrentHashMap<String, Object> cacheMap = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> containsKey(String key) {
        return Mono.just(cacheMap.containsKey(key));
    }

    @Override
    public Mono<Object> get(String key) {
        return Mono.justOrEmpty(cacheMap.get(key));
    }

    @Override
    public Mono<Boolean> put(String key, Object value) {
        Object put = cacheMap.put(key, value);
        boolean result = put != null;
        return Mono.just(result);
    }

    @Override
    public Mono<Boolean> remove(String key) {
        Object remove = cacheMap.remove(key);
        boolean result = remove != null;
        return Mono.just(result);
    }

    @Override
    public Mono<String> clear() {
        cacheMap.clear();
        return Mono.just("SUCCESS");
    }
}
