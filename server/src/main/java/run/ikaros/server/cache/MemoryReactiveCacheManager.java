package run.ikaros.server.cache;

import java.util.Iterator;
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
        cacheMap.put(key, value);
        return Mono.just(true);
    }

    @Override
    public Mono<Boolean> remove(String key) {
        Object remove = cacheMap.remove(key);
        boolean result = remove != null;
        return Mono.just(result);
    }

    @Override
    public Mono<Boolean> removePrefix(String keyPrefix) {
        boolean removedAny = false;
        Iterator<String> it = cacheMap.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (key.startsWith(keyPrefix)) {
                it.remove();  // 迭代器删除更安全
                removedAny = true;
            }
        }
        return Mono.just(removedAny);
    }

    @Override
    public Mono<String> clear() {
        cacheMap.clear();
        return Mono.just("SUCCESS");
    }
}
