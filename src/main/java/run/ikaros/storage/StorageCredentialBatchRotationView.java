package run.ikaros.storage;

public record StorageCredentialBatchRotationView(String encryptionKeyVersion,
                                                 int processedProviders, int rotatedProviders,
                                                 int rotatedFields) {
}
