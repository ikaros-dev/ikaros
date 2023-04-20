package run.ikaros.api.custom;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Custom {
    /**
     * custom group.
     */
    String group();

    /**
     * custom version.
     */
    String version();

    /**
     * custom kind.
     */
    String kind();

    /**
     * Custom singular.
     */
    String singular();

    /**
     * Custom plural.
     */
    String plural();
}
