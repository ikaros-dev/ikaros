package run.ikaros.server.cache;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.cache.CacheEvict;
import run.ikaros.api.cache.CachePut;
import run.ikaros.api.cache.Cacheable;
import run.ikaros.server.infra.utils.JsonUtils;

@Aspect
@Component
public class CacheAspect {

    @Pointcut("@annotation(run.ikaros.api.cache.Cacheable)")
    public void cacheableMethods() {
    }

    @Pointcut("@annotation(run.ikaros.api.cache.CacheEvict)")
    public void cacheEvictMethods() {
    }

    @Pointcut("@annotation(run.ikaros.api.cache.CachePut)")
    public void cachePutMethods() {
    }


    private final ReactiveCacheManager reactiveCacheManager;

    public CacheAspect(ReactiveCacheManager reactiveCacheManager) {
        this.reactiveCacheManager = reactiveCacheManager;
    }


    /**
     * 处理可缓存注解切面.
     */
    @Around("cacheableMethods() && @annotation(cacheable)")
    public Mono<Void> aroundCacheableMethods(ProceedingJoinPoint joinPoint, Cacheable cacheable)
        throws Throwable {
        StringBuilder builder = new StringBuilder();
        builder.append(Arrays.toString(cacheable.value()));
        for (Object arg : joinPoint.getArgs()) {
            builder.append(arg.toString());
        }
        final String key = builder.toString();

        return reactiveCacheManager.get(key)
            .switchIfEmpty(
                Mono.just(joinPoint.proceed())
                    .mapNotNull(JsonUtils::obj2Json)
                    .flatMap(v -> reactiveCacheManager.put(key, v)
                        .then(Mono.defer(() -> Mono.just(v))))
            )
            .then();
    }

    /**
     * 处理缓存移除注解切面.
     */
    @Around("cacheEvictMethods() && @annotation(cacheEvict)")
    public Mono<Void> aroundCacheEvictMethods(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict)
        throws Throwable {
        StringBuilder builder = new StringBuilder();
        builder.append(Arrays.toString(cacheEvict.value()));
        for (Object arg : joinPoint.getArgs()) {
            builder.append(arg.toString());
        }
        final String key = builder.toString();

        joinPoint.proceed();

        return reactiveCacheManager.remove(key).then();
    }

    /**
     * 处理缓存移除注解切面.
     */
    @Around("cachePutMethods() && @annotation(cachePut)")
    public Mono<Void> aroundCachePutMethods(ProceedingJoinPoint joinPoint, CachePut cachePut)
        throws Throwable {
        StringBuilder builder = new StringBuilder();
        builder.append(Arrays.toString(cachePut.value()));
        for (Object arg : joinPoint.getArgs()) {
            builder.append(arg.toString());
        }
        final String key = builder.toString();

        Object proceed = joinPoint.proceed();
        String value = JsonUtils.obj2Json(proceed);

        return reactiveCacheManager.put(key, value).then();
    }
}
