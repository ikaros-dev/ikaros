package run.ikaros.server.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveServerCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RedisReactiveCacheManagerTest {

    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, Object> valueOperations;

    private RedisReactiveCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheManager = new RedisReactiveCacheManager(redisTemplate);
    }

    @Test
    void containsKeyDelegatesToRedisTemplate() {
        when(redisTemplate.hasKey("testKey"))
            .thenReturn(Mono.just(true));

        cacheManager.containsKey("testKey")
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        verify(redisTemplate).hasKey("testKey");
    }

    @Test
    void containsKeyReturnsFalseWhenNotPresent() {
        when(redisTemplate.hasKey("missing"))
            .thenReturn(Mono.just(false));

        cacheManager.containsKey("missing")
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void getDelegatesToRedisTemplate() {
        when(valueOperations.get("testKey"))
            .thenReturn(Mono.just("testValue"));

        cacheManager.get("testKey")
            .as(StepVerifier::create)
            .expectNext("testValue")
            .verifyComplete();

        verify(valueOperations).get("testKey");
    }

    @Test
    void getReturnsEmptyWhenKeyNotFound() {
        when(valueOperations.get("missing"))
            .thenReturn(Mono.empty());

        cacheManager.get("missing")
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    void putDelegatesToRedisTemplate() {
        when(valueOperations.set("testKey", "testValue"))
            .thenReturn(Mono.just(true));

        cacheManager.put("testKey", "testValue")
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        verify(valueOperations).set("testKey", "testValue");
    }

    @Test
    void removeDelegatesToRedisTemplate() {
        when(valueOperations.delete("testKey"))
            .thenReturn(Mono.just(true));

        cacheManager.remove("testKey")
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        verify(valueOperations).delete("testKey");
    }

    @Test
    void removePrefixUsesKeysThenDelete() {
        when(redisTemplate.keys("prefix_**"))
            .thenReturn(Flux.just("prefix_a", "prefix_b"));
        when(redisTemplate.delete(anyString()))
            .thenReturn(Mono.just(1L));

        cacheManager.removePrefix("prefix_")
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        verify(redisTemplate).keys("prefix_**");
    }

    @Test
    void clearUsesConnectionFactoryChain() {
        ReactiveRedisConnectionFactory connectionFactory =
            mock(ReactiveRedisConnectionFactory.class);
        ReactiveRedisConnection connection =
            mock(ReactiveRedisConnection.class);
        ReactiveServerCommands serverCommands =
            mock(ReactiveServerCommands.class);

        when(redisTemplate.getConnectionFactory())
            .thenReturn(connectionFactory);
        when(connectionFactory.getReactiveConnection())
            .thenReturn(connection);
        when(connection.serverCommands())
            .thenReturn(serverCommands);
        when(serverCommands.flushAll())
            .thenReturn(Mono.just("OK"));

        cacheManager.clear()
            .as(StepVerifier::create)
            .expectNext("OK")
            .verifyComplete();

        verify(serverCommands).flushAll();
    }
}
