package run.ikaros.server.constants;

import java.io.File;

/**
 * @author guohao
 * @date 2022/09/26
 */
public interface AppConst {
    interface OpenAPI {
        String PREFIX_NAME = "/api";
        String PACKAGE_NAME = "run.ikaros.server.openapi";
    }

    interface Directory {
        String DEFAULT_UPLOAD_NAME = "upload";
    }

    String DEFAULT_THEME = "simple";
    String PAGE_POSTFIX = "themes" + File.separator + DEFAULT_THEME + File.separator;
}
