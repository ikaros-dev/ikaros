package run.ikaros.server.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class MemoryReactiveCacheManagerTest {

    private MemoryReactiveCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new MemoryReactiveCacheManager();
    }

    @Test
    void putAndGetValue() {
        cacheManager.put("key1", "value1")
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();

        cacheManager.get("key1")
            .as(StepVerifier::create)
            .expectNext("value1")
            .verifyComplete();
    }

    @Test
    void containsKeyForExistingKey() {
        cacheManager.put("existing", "data")
            .block();

        cacheManager.containsKey("existing")
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void containsKeyForNonExistingKey() {
        cacheManager.containsKey("nonExisting")
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void removeExistingKey() {
        cacheManager.put("toRemove", "data")
            .block();

        cacheManager.remove("toRemove")
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        cacheManager.containsKey("toRemove")
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void removeNonExistingKey() {
        cacheManager.remove("nonExisting")
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void removePrefixRemovesMatchingKeys() {
        cacheManager.put("prefix_a", "1").block();
        cacheManager.put("prefix_b", "2").block();
        cacheManager.put("other_c", "3").block();

        cacheManager.removePrefix("prefix_")
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        cacheManager.containsKey("prefix_a")
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();

        cacheManager.containsKey("prefix_b")
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();

        cacheManager.containsKey("other_c")
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void removePrefixNoMatch() {
        cacheManager.put("key1", "value1").block();

        cacheManager.removePrefix("zzz_")
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        cacheManager.containsKey("key1")
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void clearRemovesAllKeys() {
        cacheManager.put("k1", "v1").block();
        cacheManager.put("k2", "v2").block();
        cacheManager.put("k3", "v3").block();

        cacheManager.clear()
            .as(StepVerifier::create)
            .expectNext("SUCCESS")
            .verifyComplete();

        cacheManager.containsKey("k1")
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();

        cacheManager.containsKey("k2")
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();

        cacheManager.containsKey("k3")
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void overwriteExistingKey() {
        cacheManager.put("key", "oldValue")
            .as(StepVerifier::create)
            .expectNext(false)
            .verifyComplete();

        cacheManager.put("key", "newValue")
            .as(StepVerifier::create)
            .expectNext(true)
            .verifyComplete();

        cacheManager.get("key")
            .as(StepVerifier::create)
            .expectNext("newValue")
            .verifyComplete();
    }

    @Test
    void getNonExistingKeyReturnsEmpty() {
        cacheManager.get("missing")
            .as(StepVerifier::create)
            .verifyComplete();
    }

    @Test
    void getWithNullKeyThrowsException() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> cacheManager.get(null).block())
            .isInstanceOf(NullPointerException.class);
    }
}
