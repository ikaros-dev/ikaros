package run.ikaros.plugin;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** 插件包的声明式身份、兼容性和扩展能力。 */
public record PluginManifest(
    @NotBlank String pluginId,
    @NotBlank String name,
    @NotBlank String version,
    @NotBlank String publisher,
    @NotBlank String pluginApiVersion,
    @NotBlank String minimumServerVersion,
    String maximumServerVersion,
    @NotBlank String entrypoint,
    List<String> capabilities,
    List<String> permissions,
    List<String> extensionPoints
) {
    public PluginManifest {
        capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
        permissions = List.copyOf(permissions == null ? List.of() : permissions);
        extensionPoints = List.copyOf(extensionPoints == null ? List.of() : extensionPoints);
    }
}
