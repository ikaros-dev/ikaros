package run.ikaros.appruntime.api;

/** Server App 在当前 Ikaros Instance 中的生命周期状态。 */
public enum AppLifecycleState {
    DISCOVERED,
    INSTALLING,
    INSTALLED,
    ENABLING,
    ENABLED,
    DISABLING,
    DISABLED,
    UPGRADING,
    FAILED,
    UNINSTALLING,
    UNINSTALLED,
    INCOMPATIBLE
}
