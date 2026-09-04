package run.ikaros.storage;

import java.util.UUID;

public record StorageCredentialRotationView(UUID providerId, String providerKey,
                                            String encryptionKeyVersion, int rotatedFields) {
}
