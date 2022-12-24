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

    String DEFAULT_THEME = "simple";
    String PAGE_POSTFIX = "themes";
    String ORIGINAL = "original";
    String MEDIA = "media";
    String UPLOAD = "upload";
    String DOWNLOADS = "downloads";
    String IMPORT = "import";
}
