package run.ikaros.server.cache;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
@Component
public class CacheAspect {


    @Pointcut("@annotation(run.ikaros.server.cache.MonoCacheable) "
        + "&& execution(public reactor.core.publisher.Mono *(..))")
    public void monoCacheableMethods() {
    }

    @Pointcut("@annotation(run.ikaros.server.cache.FluxCacheable) "
        + "&& execution(public reactor.core.publisher.Flux *(..))")
    public void fluxCacheableMethods() {
    }

    @Pointcut("@annotation(run.ikaros.server.cache.MonoCacheEvict)")
    public void monoCacheEvictMethods() {
    }

    @Pointcut("@annotation(run.ikaros.server.cache.FluxCacheEvict)")
    public void fluxCacheEvictMethods() {
    }

    private final ExpressionParser spelExpressionParser = new SpelExpressionParser();
    private final ConcurrentHashMap<String, Class<?>> methodReturnValueTypes
        = new ConcurrentHashMap<>();

    private final ReactiveCacheManager cm;

    public CacheAspect(ReactiveCacheManager cm) {
        this.cm = cm;
    }

    /**
     * 应用关闭时清空缓存.
     */
    // @PreDestroy
    public void onShutdown() throws InterruptedException {
        // 使用 CountDownLatch 来确保响应式流在退出前执行完成
        CountDownLatch latch = new CountDownLatch(1);

        cm.clear().then()
            .doOnTerminate(latch::countDown)  // 当任务完成时计数器减一
            .subscribe();

        // 等待响应式操作完成
        latch.await();
        System.out.println("Shutdown process completed.");
    }


    private String parseSpelExpression(String expression, ProceedingJoinPoint joinPoint) {
        final EvaluationContext context = new StandardEvaluationContext();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = methodSignature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], paramValues[i]);
        }
        return spelExpressionParser.parseExpression(expression).getValue(context, String.class);
    }

    /**
     * 处理可缓存注解切面
     * 要求返回值为Mono类型
     * .
     */
    @Around("monoCacheableMethods() && @annotation(monoCacheable)")
    public Mono<?> aroundMonoMethodsWithAnnotationCacheable(
        ProceedingJoinPoint joinPoint, MonoCacheable monoCacheable) throws Throwable {
        final String cacheKeyPostfix = parseSpelExpression(monoCacheable.key(), joinPoint);
        final List<String> cacheKeys =
            Arrays.stream(monoCacheable.value())
                .map(namespace -> namespace + cacheKeyPostfix).toList();
        return Flux.fromStream(cacheKeys.stream())
            .concatMap(key -> cm.get(key).filter(Objects::nonNull))
            .next()
            // 缓存中不存在
            .switchIfEmpty(Mono.defer(() -> {
                Object proceed;
                try {
                    proceed = joinPoint.proceed(joinPoint.getArgs());
                } catch (Throwable e) {
                    return Mono.error(e);
                }
                return ((Mono<?>) proceed)
                    .flatMap(val ->
                        Flux.fromIterable(cacheKeys)
                            .flatMap(k -> cm.put(k, val))
                            .next()
                            .flatMap(list -> Mono.just(val))
                    ).switchIfEmpty(
                        Flux.fromIterable(cacheKeys)
                            .flatMap(k -> cm.put(k, "null"))
                            .next()
                            .flatMap(bool -> Mono.empty())
                    );
            }))
            .map(o -> {
                if (o instanceof Integer integer) {
                    return integer.longValue();
                }
                return o;
            })
            .filter(o -> !"null".equals(o));
    }

    /**
     * 处理可缓存注解切面
     * 要求返回值为Flux类型
     * .
     */
    @Around("fluxCacheableMethods() && @annotation(fluxCacheable)")
    public Flux<?> aroundMonoMethodsWithAnnotationCacheable(
        ProceedingJoinPoint joinPoint, FluxCacheable fluxCacheable) throws Throwable {
        final String cacheKeyPostfix = parseSpelExpression(fluxCacheable.key(), joinPoint);
        final List<String> cacheKeys =
            Arrays.stream(fluxCacheable.value())
                .map(namespace -> namespace + cacheKeyPostfix).toList();
        return Flux.fromStream(cacheKeys.stream())
            .concatMap(key -> cm.get(key)
                .filter(Objects::nonNull))
            .next()
            // 缓存中不存在
            .switchIfEmpty(Mono.defer(() -> {
                Object proceed;
                try {
                    proceed = joinPoint.proceed(joinPoint.getArgs());
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }

                return ((Flux<?>) proceed)
                    .collectList()
                    .flatMap(vals ->
                        Flux.fromIterable(cacheKeys)
                            .flatMap(k -> cm.put(k, vals))
                            .collectList()
                            .flatMap(list -> Mono.just(vals))
                    ).switchIfEmpty(
                        Flux.fromIterable(cacheKeys)
                            .flatMap(k -> cm.put(k, List.of()))
                            .next()
                            .flatMap(bool -> Mono.empty())
                    );
            }))
            .map(o -> (List<?>) o)
            .filter(list -> !list.isEmpty())
            // 缓存中存的是集合
            .flatMapMany(Flux::fromIterable);
    }

    /**
     * 处理缓存移除注解切面
     * 要求返回值为Mono类型
     * .
     */
    @Around("monoCacheEvictMethods() && @annotation(monoCacheEvict)")
    public Mono<?> aroundMonoMethodsWithAnnotationCacheable(
        ProceedingJoinPoint joinPoint, MonoCacheEvict monoCacheEvict
    ) throws Throwable {
        Object proceed = joinPoint.proceed();
        if (monoCacheEvict.value().length == 0
            && "".equals(monoCacheEvict.key())) {
            return cm.clear()
                .flatMap(s -> (Mono<?>) proceed);
        }
        final String cacheKeyPostfix = parseSpelExpression(monoCacheEvict.key(), joinPoint);
        final List<String> cacheKeys =
            Arrays.stream(monoCacheEvict.value())
                .map(namespace -> namespace + cacheKeyPostfix).toList();
        return Flux.fromStream(cacheKeys.stream())
            .flatMap(cm::remove)
            .next()
            .flatMap(bool -> (Mono<?>) proceed);
    }

    /**
     * 处理缓存移除注解切面
     * 要求返回值为Mono类型
     * .
     */
    @Around("fluxCacheEvictMethods() && @annotation(fluxCacheEvict)")
    public Flux<?> aroundMonoMethodsWithAnnotationCacheable(
        ProceedingJoinPoint joinPoint, FluxCacheEvict fluxCacheEvict
    ) throws Throwable {
        Object proceed = joinPoint.proceed();
        if (fluxCacheEvict.value().length == 0
            && "".equals(fluxCacheEvict.key())) {
            return cm.clear()
                .flatMapMany(s -> (Flux<?>) proceed);
        }
        final String cacheKeyPostfix = parseSpelExpression(fluxCacheEvict.key(), joinPoint);
        final List<String> cacheKeys =
            Arrays.stream(fluxCacheEvict.value())
                .map(namespace -> namespace + cacheKeyPostfix).toList();
        return Flux.fromStream(cacheKeys.stream())
            .flatMap(cm::remove)
            .next()
            .flatMapMany(bool -> (Flux<?>) proceed);
    }


}
