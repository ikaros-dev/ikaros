package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

/** Replaces provider credentials without requiring the previous ciphertext to be decryptable. */
@Service
public class StorageProviderCredentialService {
    private final StorageProviderRepository providers;
    private final StorageCredentialCipher cipher;

    public StorageProviderCredentialService(StorageProviderRepository providers, StorageCredentialCipher cipher) {
        this.providers = providers;
        this.cipher = cipher;
    }

    public Mono<Void> replace(UUID providerId, ReplaceStorageProviderCredentialsRequest request) {
        return providers.findById(providerId)
            .switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .flatMap(provider -> providers.save(new StorageProviderEntity(provider.id(), provider.providerKey(),
                provider.providerType(), provider.tier(), provider.status(), "secret://provider/" + provider.providerKey(),
                provider.providerMetadata().asString(), cipher.encrypt(request.accessKeyId()),
                cipher.encrypt(request.secretAccessKey()), cipher.encrypt(blankToNull(request.sessionToken())),
                provider.createdAt(), Instant.now())))
            .then();
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
}
