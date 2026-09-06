package run.ikaros.storage;

public record StorageUploadRequest(String objectKey, long sizeBytes, String mediaType, String sha256) {
    public StorageUploadRequest(String objectKey, long sizeBytes, String mediaType) {
        this(objectKey, sizeBytes, mediaType, null);
    }
    public StorageUploadRequest {
        if (objectKey == null || objectKey.isBlank()) throw new IllegalArgumentException("objectKey 不能为空");
        if (sizeBytes < 0) throw new IllegalArgumentException("sizeBytes 不能小于 0");
        if (mediaType == null || mediaType.isBlank()) throw new IllegalArgumentException("mediaType 不能为空");
        if (sha256 != null && !sha256.matches("^[A-Fa-f0-9]{64}$")) throw new IllegalArgumentException("sha256 格式无效");
    }
}
