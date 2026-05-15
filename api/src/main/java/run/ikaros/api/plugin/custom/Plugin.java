package run.ikaros.api.plugin.custom;

import io.swagger.v3.oas.annotations.media.Schema;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.pf4j.PluginState;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.custom.Custom;
import run.ikaros.api.custom.Name;

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
 * displayName: "PluginStarterDemo"
 * description: "PluginStarterDemo"
 * license: "AGPL-2.0"
 * </pre>
 */
@Data
@Custom(group = OpenApiConst.PLUGIN_GROUP, version = OpenApiConst.CORE_VERSION,
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
    /**
     * format: pluginId:pluginVersion.
     */
    private Map<String, String> dependencies = new HashMap<>(4);
    private Path loadLocation;
    /**
     * 插件入口，一般是编译后的JS文件.
     */
    private String entry;
    /**
     * 插件样式，一般是编译后的一个CSS文件.
     */
    private String stylesheet;

    @Data
    public static class Author {
        private String name;
        private String website;
    }

    /**
     * 用于生成插件配置表单，核心会读取插件目录下的[configMapSchemas]文本文件，将内容保存，
     * 同时会根据插件名称和表单里面的[name]字段值组合一个属于插件的
     * {@link run.ikaros.api.core.setting.ConfigMap}.
     *
     * @see run.ikaros.api.core.setting.ConfigMap
     * @see <a href="https://formkit.com/essentials/schema">FormKit Schema</a>
     */
    private String configMapSchemas;
}
