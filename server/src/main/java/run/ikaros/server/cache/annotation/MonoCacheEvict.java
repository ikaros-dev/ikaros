package run.ikaros.server.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.core.annotation.AliasFor;
import run.ikaros.server.cache.CacheAspect;

/**
 * 当 value或者cacheNames 和 key 啥都不填，代表清空缓存，
 * 目前不能和事务注解共用
 * .
 *
 * @see CacheAspect#monoCacheEvictMethods()
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Reflective
public @interface MonoCacheEvict {
    /**
     * 缓存命名空间.
     */
    @AliasFor("cacheNames")
    String[] value() default {};

    /**
     * 缓存命名空间.
     */
    @AliasFor("value")
    String[] cacheNames() default {};

    /**
     * 缓存KEY.
     */
    String key() default "";
}
