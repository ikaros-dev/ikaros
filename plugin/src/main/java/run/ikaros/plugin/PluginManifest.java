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
    List<String> extensionPoints,
    List<String> configurationKeys,
    List<String> secretReferences
) {
    public PluginManifest(String pluginId, String name, String version, String publisher,
                          String pluginApiVersion, String minimumServerVersion, String maximumServerVersion,
                          String entrypoint, List<String> capabilities, List<String> permissions,
                          List<String> extensionPoints) {
        this(pluginId, name, version, publisher, pluginApiVersion, minimumServerVersion, maximumServerVersion,
            entrypoint, capabilities, permissions, extensionPoints, List.of(), List.of());
    }

    public PluginManifest {
        capabilities = List.copyOf(capabilities == null ? List.of() : capabilities);
        permissions = List.copyOf(permissions == null ? List.of() : permissions);
        extensionPoints = List.copyOf(extensionPoints == null ? List.of() : extensionPoints);
        configurationKeys = List.copyOf(configurationKeys == null ? List.of() : configurationKeys);
        secretReferences = List.copyOf(secretReferences == null ? List.of() : secretReferences);
        if (secretReferences.stream().anyMatch(reference -> !reference.startsWith("secret://"))) {
            throw new IllegalArgumentException("插件 secret reference 必须使用 secret:// URI");
        }
    }
}
