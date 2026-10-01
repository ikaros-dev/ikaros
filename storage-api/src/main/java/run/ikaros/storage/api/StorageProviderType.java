package run.ikaros.storage.api;

/**
 * Storage Provider 的物理 adapter 类型；创建 Provider 时必须声明已注册的 adapter 类型，
 * 而不是由 Storage Core 猜测。
 */
public enum StorageProviderType {
    /** 通用 AWS S3 adapter。 */
    S3,
    /** AWS S3 adapter。 */
    AWS_S3,
    /** 其他 S3 兼容服务（MinIO 等）。 */
    S3_COMPATIBLE,
    /** 阿里云 OSS 的 S3 协议 adapter。 */
    ALIYUN_OSS_S3,
    /** 腾讯云 COS 的 S3 协议 adapter。 */
    TENCENT_COS_S3,
    /** 本地文件系统 adapter。 */
    LOCAL_FILESYSTEM
}
