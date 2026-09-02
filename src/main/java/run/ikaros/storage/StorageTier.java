package run.ikaros.storage;

/**
 * 持久化存储层级，不与服务器或客户端缓存层混淆。
 */
public enum StorageTier {
    HOT,
    WARM,
    COLD,
    ARCHIVE
}
