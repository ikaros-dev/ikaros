package run.ikaros.storage;

import run.ikaros.storage.api.*;

import java.util.Map;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StorageProviderRegistry {
    Mono<StorageProvider> register(String providerKey, String providerType, StorageTier tier,
                                    String secretReference, Map<String, Object> metadata);

    default Mono<StorageProvider> registerConfigured(String providerKey, String providerType, String displayName,
        StorageTier tier, String secretReference, Map<String, Object> capabilities, Map<String, Object> configuration,
        String idempotencyKey, String requestFingerprint) {
        return register(providerKey, providerType, tier, secretReference, configuration);
    }

    /**
     * 创建 Provider，并可同时写入由 Storage Owner 加密保存的对象存储凭据。
     *
     * @param accessKeyId 对象存储 Access Key ID；与 secretAccessKey 必须同时提供或同时为空
     * @param secretAccessKey 对象存储 Secret Access Key
     * @param sessionToken 可选临时 Session Token
     */
    default Mono<StorageProvider> registerConfigured(String providerKey, String providerType, String displayName,
        StorageTier tier, String secretReference, Map<String, Object> capabilities, Map<String, Object> configuration,
        String idempotencyKey, String requestFingerprint,
        String accessKeyId, String secretAccessKey, String sessionToken) {
        return registerConfigured(providerKey, providerType, displayName, tier, secretReference,
            capabilities, configuration, idempotencyKey, requestFingerprint);
    }

    default Mono<StorageProvider> register(String providerKey, String providerType, StorageTier tier,
                                           String secretReference, Map<String, Object> metadata,
                                           String accessKeyId, String secretAccessKey, String sessionToken) {
        return register(providerKey, providerType, tier, secretReference, metadata);
    }
    Mono<StorageProvider> update(UUID providerId, UpdateStorageProviderRequest request, long expectedVersion);
    Mono<StorageProvider> enable(UUID providerId);
    Mono<StorageProvider> disable(UUID providerId);
    Mono<StorageProvider> drain(UUID providerId);
    Mono<StorageProvider> get(UUID providerId);
    Mono<StorageProvider> getByKey(String providerKey);
    Flux<StorageProvider> list();
    Mono<Void> requireWritable(UUID providerId);
    Mono<StorageProvider> requireWritableByKey(String providerKey);
}
