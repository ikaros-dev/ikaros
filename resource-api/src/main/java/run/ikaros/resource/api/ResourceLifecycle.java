package run.ikaros.resource.api;

/**
 * Resource 的逻辑生命周期，物理 Blob 回收由独立 GC 决策。
 */
public enum ResourceLifecycle {
    ACTIVE,
    ARCHIVED,
    TRASHED,
    PURGED;

    /** Legacy source compatibility for callers migrating to numeric ResourceStatus. */
    public int statusCode() {
        return switch (this) {
            case PURGED -> ResourceStatus.DELETED.code();
            case ACTIVE -> ResourceStatus.ACTIVE.code();
            case TRASHED -> ResourceStatus.TRASHED.code();
            case ARCHIVED -> ResourceStatus.ARCHIVED.code();
        };
    }
}
