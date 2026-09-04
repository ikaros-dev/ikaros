package run.ikaros.storage;

public record StorageObjectMetadata(String objectKey, long sizeBytes, String mediaType, String etag, String sha256) {
    public StorageObjectMetadata(String objectKey, long sizeBytes, String mediaType, String etag) {
        this(objectKey, sizeBytes, mediaType, etag, null);
    }
}
