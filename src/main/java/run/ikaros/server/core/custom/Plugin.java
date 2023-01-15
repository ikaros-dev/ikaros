package run.ikaros.server.core.custom;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.pf4j.PluginState;
import run.ikaros.server.custom.Custom;
import run.ikaros.server.custom.Name;
import run.ikaros.server.infra.constant.OpenApiConst;

/**
 * Plugin custom model.
 * <p><code>plugin.yml</code> example: </p>
 * <pre>
 * # A unique name
 * name: plugin-starter
 * # plugin entry class that extends BasePlugin
 * clazz: run.ikaros.plugin.starter.StarterPlugin
 * # plugin 'version' is a valid semantic version string (see semver.org).
 * version: 1.0.0
 * requires: "*"
 * author:
 *   name: Ikaros OSS Team
 *   website: https://github.com/ikaros-dev
 * logo: https://github.com/ikaros-dev/ikaros/blob/master/assets/logo.png
 * homepage: https://github.com/ikaros-dev/plugin-starter
 * displayName: "插件快速开始模板"
 * description: "这是一个插件快速开始模板"
 * license: "AGPL-2.0"
 * </pre>
 */
@Data
@Custom(group = OpenApiConst.CORE_GROUP, version = OpenApiConst.CORE_VERSION,
    kind = "Plugin", singular = "plugin", plural = "plugins")
public class Plugin {

    @Name
    private String name;
    private String clazz;
    /**
     * plugin version.
     *
     * @see <a href="semver.org">semantic version</a>
     */
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
        pattern = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-("
            + "(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\."
            + "(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\"
            + ".[0-9a-zA-Z-]+)*))?$",
        example = "1.0.0")
    private String version;
    private String requires;
    private Author author;
    private String logo;
    private String homepage;
    private String displayName;
    private String description;
    private String license;
    private PluginState state;
    private Map<String, String> dependencies = new HashMap<>(4);

    @Data
    public static class Author {
        private String name;
        private String website;
    }
}
