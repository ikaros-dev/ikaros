package run.ikaros.server.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Extension {
    /**
     * extension group.
     */
    String group();

    /**
     * extension version.
     */
    String version();

    /**
     * extension kind.
     */
    String kind();

}
