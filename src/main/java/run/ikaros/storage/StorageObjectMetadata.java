package run.ikaros.storage;

public record StorageObjectMetadata(String objectKey, long sizeBytes, String mediaType, String etag) { }
