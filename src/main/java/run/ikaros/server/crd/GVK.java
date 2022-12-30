package run.ikaros.server.crd;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * GVK: Group Version Kind
 *
 * @author: li-guohao
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface GVK {
    String group();

    String version();

    String kind();

    String plural();

    String singular();
}
