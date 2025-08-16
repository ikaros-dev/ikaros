package run.ikaros.server.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.core.annotation.AliasFor;

/**
 * 数字类型返回值，统一用 Mono Long 接收，
 * 请加在接口具体实现类的方法上，不要加在接口的方法上.
 * .
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Reflective
public @interface MonoCacheable {

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
