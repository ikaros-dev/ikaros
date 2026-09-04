package run.ikaros.storage;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class StorageProviderCredentialRotationService {
    private static final int MAX_BATCH_PROVIDERS = 100;
    private final StorageProviderRepository providers;
    private final StorageCredentialCipher cipher;

    public StorageProviderCredentialRotationService(StorageProviderRepository providers, StorageCredentialCipher cipher) {
        this.providers = providers;
        this.cipher = cipher;
    }

    public Mono<StorageCredentialRotationView> rotate(UUID providerId) {
        return providers.findById(providerId)
            .switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .flatMap(this::rotate);
    }

    public Mono<StorageCredentialBatchRotationView> rotateAll() {
        return providers.findAll()
            .take(MAX_BATCH_PROVIDERS)
            .concatMap(this::rotate)
            .collectList()
            .map(results -> new StorageCredentialBatchRotationView(cipher.activeKeyVersion(), results.size(),
                (int) results.stream().filter(result -> result.rotatedFields() > 0).count(),
                results.stream().mapToInt(StorageCredentialRotationView::rotatedFields).sum()));
    }

    private Mono<StorageCredentialRotationView> rotate(StorageProviderEntity provider) {
        if (provider.accessKeyIdCiphertext() == null || provider.secretAccessKeyCiphertext() == null) {
            return Mono.error(new ConflictException("Provider 未配置 AccessKey/SecretKey"));
        }
        String access = cipher.reEncrypt(provider.accessKeyIdCiphertext());
        String secret = cipher.reEncrypt(provider.secretAccessKeyCiphertext());
        String session = cipher.reEncrypt(provider.sessionTokenCiphertext());
        int rotated = changed(provider.accessKeyIdCiphertext(), access)
            + changed(provider.secretAccessKeyCiphertext(), secret)
            + changed(provider.sessionTokenCiphertext(), session);
        if (rotated == 0) {
            return Mono.just(view(provider, 0));
        }
        return providers.save(new StorageProviderEntity(provider.id(), provider.providerKey(), provider.providerType(),
                provider.tier(), provider.status(), provider.secretReference(), provider.providerMetadata(), access,
                secret, session, provider.createdAt(), Instant.now()))
            .map(saved -> view(saved, rotated));
    }

    private int changed(String before, String after) { return Objects.equals(before, after) ? 0 : 1; }

    private StorageCredentialRotationView view(StorageProviderEntity provider, int rotated) {
        return new StorageCredentialRotationView(provider.id(), provider.providerKey(), cipher.activeKeyVersion(), rotated);
    }
}
