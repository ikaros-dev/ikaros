package run.ikaros.server.cache;


import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

public class RedisReactiveCacheManager implements ReactiveCacheManager {
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    public RedisReactiveCacheManager(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Boolean> containsKey(String key) {
        return redisTemplate.hasKey(key);
    }

    @Override
    public Mono<Object> get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public Mono<Boolean> put(String key, Object value) {
        return redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public Mono<Boolean> remove(String key) {
        return redisTemplate.opsForValue().delete(key);
    }

    @Override
    public Mono<Boolean> removePrefix(String keyPrefix) {
        return redisTemplate.keys(keyPrefix + "**")
        .flatMap(redisTemplate::delete)
        .then(Mono.just(true));
    }

    @Override
    public Mono<String> clear() {
        return redisTemplate.getConnectionFactory().getReactiveConnection()
            .serverCommands().flushAll();
    }
}
