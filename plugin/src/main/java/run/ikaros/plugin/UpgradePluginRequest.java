package run.ikaros.plugin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UpgradePluginRequest(@Valid @NotNull PluginManifest manifest, Set<String> grantedPermissions) { }
