package run.ikaros.plugin;

import jakarta.validation.constraints.NotNull;

public record UninstallPluginRequest(@NotNull PluginUninstallPolicy retention) { }
