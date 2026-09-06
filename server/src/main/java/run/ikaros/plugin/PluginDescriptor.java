package run.ikaros.plugin;

import java.util.Set;

/** 已安装插件及其显式授权的运行时视图。 */
public record PluginDescriptor(PluginManifest manifest, PluginLifecycle lifecycle,
                               Set<String> grantedPermissions) {
    public PluginDescriptor {
        grantedPermissions = Set.copyOf(grantedPermissions == null ? Set.of() : grantedPermissions);
    }
}
