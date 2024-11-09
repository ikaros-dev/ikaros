package run.ikaros.api.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.core.annotation.AliasFor;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Reflective
public @interface Cacheable {

    /**
     * 缓存名称.
     */
    @AliasFor("cacheNames")
    String[] value() default {};

    /**
     * 缓存名称.
     */
    String[] cacheNames() default {};

    /**
     * 缓存键.
     */
    String key() default "";
}
