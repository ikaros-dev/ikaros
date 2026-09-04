package run.ikaros.storage;

public record StorageUploadRequest(String objectKey, long sizeBytes, String mediaType) {
    public StorageUploadRequest {
        if (objectKey == null || objectKey.isBlank()) throw new IllegalArgumentException("objectKey 不能为空");
        if (sizeBytes < 0) throw new IllegalArgumentException("sizeBytes 不能小于 0");
        if (mediaType == null || mediaType.isBlank()) throw new IllegalArgumentException("mediaType 不能为空");
    }
}
