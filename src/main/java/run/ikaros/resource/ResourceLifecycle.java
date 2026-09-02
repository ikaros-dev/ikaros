package run.ikaros.resource;

/**
 * Resource 的逻辑生命周期，物理 Blob 回收由独立 GC 决策。
 */
public enum ResourceLifecycle {
    ACTIVE,
    ARCHIVED,
    TRASHED
}
