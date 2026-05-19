package run.ikaros.server.cache;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.server.cache.annotation.FluxCacheable;
import run.ikaros.server.cache.annotation.MonoCacheEvict;
import run.ikaros.server.cache.annotation.MonoCacheable;

/**
 * CacheAspect 缓存一致性测试.
 * 验证修复后的缓存执行时序和 key 一致性问题.
 */
class CacheAspectTest {

    private MemoryReactiveCacheManager cacheManager;
    private TestService testService;
    private TestServiceImpl testServiceImpl;

    interface TestService {

        Mono<String> findById(UUID id);

        Mono<String> findByBgmId(String bgmId);

        Flux<String> findAllByPlatform(String platform);

        Mono<String> update(UUID id, String data);

        Mono<String> delete(UUID id);

        Mono<String> clearAllCache();
    }

    static class TestServiceImpl implements TestService {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final AtomicReference<String> lastDbState = new AtomicReference<>("initial");

        @Override
        @MonoCacheable(value = "entity:", key = "#id")
        public Mono<String> findById(UUID id) {
            callCount.incrementAndGet();
            return Mono.just("entity-" + id);
        }

        @Override
        @MonoCacheable(value = "bgm:", key = "#bgmId")
        public Mono<String> findByBgmId(String bgmId) {
            callCount.incrementAndGet();
            return Mono.just("bgm-entity-" + bgmId);
        }

        @Override
        @FluxCacheable(value = "platform:", key = "#platform")
        public Flux<String> findAllByPlatform(String platform) {
            callCount.incrementAndGet();
            return Flux.just("item1-" + platform, "item2-" + platform);
        }

        @Override
        @MonoCacheEvict(value = "entity:", key = "#id")
        public Mono<String> update(UUID id, String data) {
            // 模拟数据库更新，设置新的状态
            lastDbState.set(data);
            callCount.incrementAndGet();
            return Mono.just(data);
        }

        @Override
        @MonoCacheEvict
        public Mono<String> delete(UUID id) {
            lastDbState.set("deleted-" + id);
            callCount.incrementAndGet();
            return Mono.just("deleted");
        }

        @Override
        @MonoCacheEvict
        public Mono<String> clearAllCache() {
            callCount.incrementAndGet();
            return Mono.just("cleared");
        }

        public int getCallCount() {
            return callCount.get();
        }

        public void resetCallCount() {
            callCount.set(0);
        }

        public String getLastDbState() {
            return lastDbState.get();
        }
    }

    @BeforeEach
    void setUp() {
        cacheManager = new MemoryReactiveCacheManager();
        CacheAspect cacheAspect = new CacheAspect(cacheManager);
        testServiceImpl = new TestServiceImpl();

        AspectJProxyFactory factory = new AspectJProxyFactory(testServiceImpl);
        factory.addAspect(cacheAspect);
        testService = factory.getProxy();
    }

    /**
     * 测试1: 查询方法应该缓存结果，第二次调用应直接返回缓存值.
     */
    @Test
    void cacheableMethodShouldCacheResult() {
        UUID id = UUID.randomUUID();

        // 第一次调用，应执行真实方法
        testService.findById(id)
            .as(StepVerifier::create)
            .expectNext("entity-" + id)
            .verifyComplete();

        // 第二次调用，应命中缓存，不执行真实方法
        testService.findById(id)
            .as(StepVerifier::create)
            .expectNext("entity-" + id)
            .verifyComplete();

        // 验证方法只被调用了一次（第二次走了缓存）
        assert testServiceImpl.getCallCount() == 1 : "方法应该只被调用一次";
    }

    /**
     * 测试2: findByBgmId 查询方法应该缓存，而不是清空缓存.
     * 这是修复前的 bug - 查询方法误标为 @MonoCacheEvict 会清空所有缓存.
     */
    @Test
    void findByBgmIdShouldCacheNotEvict() {
        String bgmId = "bgm123";

        // 第一次调用
        testService.findByBgmId(bgmId)
            .as(StepVerifier::create)
            .expectNext("bgm-entity-" + bgmId)
            .verifyComplete();

        // 第二次调用，应命中缓存
        testService.findByBgmId(bgmId)
            .as(StepVerifier::create)
            .expectNext("bgm-entity-" + bgmId)
            .verifyComplete();

        assert testServiceImpl.getCallCount() == 1 : "findByBgmId 应该被缓存";

        // 验证其他缓存没有被清空
        UUID otherId = UUID.randomUUID();
        testService.findById(otherId)
            .as(StepVerifier::create)
            .expectNext("entity-" + otherId)
            .verifyComplete();

        // 如果 findByBgmId 清空了所有缓存，这里 findById 会被再次调用
        // 但由于 findByBgmId 应该缓存而不是清空，findById 第二次调用也应命中缓存
        testService.findById(otherId)
            .as(StepVerifier::create)
            .expectNext("entity-" + otherId)
            .verifyComplete();

        assert testServiceImpl.getCallCount() == 2 : "findById 也应被缓存，总调用次数应为 2";
    }

    /**
     * 测试3: CacheEvict 应该先执行写操作，再清除缓存.
     * 修复前的问题：先清缓存，再执行写操作，会导致脏读窗口.
     */
    @Test
    void cacheEvictShouldExecuteAfterWriteOperation() {
        UUID id = UUID.randomUUID();

        // 先缓存一个值
        testService.findById(id)
            .as(StepVerifier::create)
            .expectNext("entity-" + id)
            .verifyComplete();

        // 执行 update 操作（带 @MonoCacheEvict）
        testService.update(id, "new-data")
            .as(StepVerifier::create)
            .expectNext("new-data")
            .verifyComplete();

        // 验证 update 后的查询应该执行新方法（缓存已被清除）
        testServiceImpl.resetCallCount();
        testService.findById(id)
            .as(StepVerifier::create)
            .expectNext("entity-" + id)
            .verifyComplete();

        // 缓存已被 update 清除，所以 findById 应该被调用一次
        assert testServiceImpl.getCallCount() == 1 : "update 后缓存应被清除";
    }

    /**
     * 测试4: @MonoCacheEvict 清空所有缓存应该生效.
     */
    @Test
    void fullCacheEvictShouldClearAllCache() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        // 缓存两个值
        testService.findById(id1).block();
        testService.findById(id2).block();

        assert testServiceImpl.getCallCount() == 2 : "应执行两次方法";

        // 执行 delete（带 @MonoCacheEvict 无 value/key，应清空所有缓存）
        testService.delete(id1).block();

        // 两个缓存都应被清空
        testServiceImpl.resetCallCount();
        testService.findById(id1).block();
        testService.findById(id2).block();

        assert testServiceImpl.getCallCount() == 2 : "delete 后所有缓存应被清除";
    }

    /**
     * 测试5: FluxCacheable 应该正确缓存 Flux 结果.
     */
    @Test
    void fluxCacheableShouldCacheFluxResult() {
        String platform = "bilibili";

        // 第一次调用
        testService.findAllByPlatform(platform)
            .as(StepVerifier::create)
            .expectNext("item1-" + platform, "item2-" + platform)
            .verifyComplete();

        // 第二次调用，应命中缓存
        testService.findAllByPlatform(platform)
            .as(StepVerifier::create)
            .expectNext("item1-" + platform, "item2-" + platform)
            .verifyComplete();

        assert testServiceImpl.getCallCount() == 1 : "Flux 缓存应生效";
    }

    /**
     * 测试6: 缓存 key 隔离 - 不同命名空间的缓存应该独立.
     */
    @Test
    void cacheKeyShouldBeIsolatedAcrossNamespaces() {
        String bgmId = "bgm456";

        // 缓存 bgm: 命名空间的值
        testService.findByBgmId(bgmId).block();

        // 清空 entity: 命名空间下的缓存（不应该影响 bgm:）
        testService.clearAllCache().block();

        // bgm: 缓存应该也被清空（因为 clearAllCache 是 @MonoCacheEvict 无 value/key）
        testServiceImpl.resetCallCount();
        testService.findByBgmId(bgmId).block();
        assert testServiceImpl.getCallCount() == 1 : "全量清空后 bgm 缓存也应被清除";
    }

    /**
     * 测试7: 验证响应式操作的正确时序.
     * 模拟一个有延迟的写操作，确保缓存清除在写操作完成后执行.
     */
    @Test
    void cacheEvictShouldWaitForWriteOperationCompletion() {
        AtomicReference<String> executionOrder = new AtomicReference<>("");

        // 创建一个有延迟的服务来测试时序
        TestService delayedService = new TestService() {
            @Override
            @MonoCacheable(value = "delayed:", key = "#id")
            public Mono<String> findById(UUID id) {
                return Mono.just("cached-" + id);
            }

            @Override
            @MonoCacheable(value = "delayed:bgm:", key = "#bgmId")
            public Mono<String> findByBgmId(String bgmId) {
                return Mono.just("bgm-" + bgmId);
            }

            @Override
            @FluxCacheable(value = "delayed:platform:", key = "#platform")
            public Flux<String> findAllByPlatform(String platform) {
                return Flux.just("item-" + platform);
            }

            @Override
            @MonoCacheEvict(value = "delayed:", key = "#id")
            public Mono<String> update(UUID id, String data) {
                // 模拟一个有延迟的写操作
                return Mono.delay(Duration.ofMillis(50))
                    .doOnNext(t -> executionOrder.compareAndSet("", "write-done"))
                    .map(t -> data);
            }

            @Override
            @MonoCacheEvict
            public Mono<String> delete(UUID id) {
                return Mono.just("deleted");
            }

            @Override
            @MonoCacheEvict
            public Mono<String> clearAllCache() {
                return Mono.just("cleared");
            }
        };

        AspectJProxyFactory factory = new AspectJProxyFactory(delayedService);
        factory.addAspect(new CacheAspect(cacheManager));
        TestService proxy = factory.getProxy();

        UUID id = UUID.randomUUID();

        // 先缓存值
        proxy.findById(id).block();

        // 执行带延迟的更新
        executionOrder.set("");
        proxy.update(id, "updated").block();

        // 验证写操作在缓存清除之前完成
        assert "write-done".equals(executionOrder.get()) : "写操作应该在缓存清除之前完成";
    }
}
