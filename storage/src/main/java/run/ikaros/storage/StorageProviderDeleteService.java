package run.ikaros.storage;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.operations.api.AuditActorType;
import run.ikaros.operations.api.AuditEventCommand;
import run.ikaros.operations.api.AuditResult;
import run.ikaros.operations.api.AuditRiskLevel;
import run.ikaros.operations.api.AuditService;

@Service
public class StorageProviderDeleteService {
    private final StorageProviderRepository providers;
    private final BlobPlacementRepository placements;
    private final MediaDeliveryBindingRepository bindings;
    private final AuditService audit;
    private final DurableEventPublisher events;
    private final TransactionalOperator transaction;

    public StorageProviderDeleteService(StorageProviderRepository providers, BlobPlacementRepository placements,
        MediaDeliveryBindingRepository bindings, AuditService audit, DurableEventPublisher events,
        TransactionalOperator transaction) {
        this.providers = providers;
        this.placements = placements;
        this.bindings = bindings;
        this.audit = audit;
        this.events = events;
        this.transaction = transaction;
    }

    public Mono<Void> delete(UUID actorId, UUID providerId, long expectedVersion) {
        return providers.findById(providerId)
            .switchIfEmpty(Mono.error(new NotFoundException("Storage Provider 不存在")))
            .flatMap(provider -> {
                if (provider.version() != expectedVersion) {
                    return Mono.error(new run.ikaros.common.PreconditionFailedException("Storage Provider 版本已变更，请刷新后重试"));
                }
                if (!Boolean.FALSE.equals(provider.enabled())) {
                    return Mono.error(new ConflictException("请先停用 Storage Provider 再删除"));
                }
                return Mono.zip(placements.countByProvider(provider.providerKey()),
                        bindings.countByStorageProviderId(provider.id()))
                    .flatMap(counts -> {
                        if (counts.getT1() > 0 || counts.getT2() > 0) {
                            return Mono.error(new ConflictException("Storage Provider 仍有关联的数据位置或 Delivery Binding"));
                        }
                        String details = "{\"provider_key\":\"" + safe(provider.providerKey()) + "\"}";
                        String payload = "{\"provider_id\":\"" + provider.id() + "\",\"provider_key\":\""
                            + safe(provider.providerKey()) + "\"}";
                        return transaction.transactional(providers.delete(provider)
                            .then(audit.record(new AuditEventCommand(AuditActorType.USER, actorId,
                                "storage.provider.delete", "STORAGE_PROVIDER", provider.id(), AuditResult.SUCCESS,
                                AuditRiskLevel.HIGH, details, 1, null)))
                            .then(events.append(new EventAppendRequest("storage.provider.deleted", 1, "storage",
                                "storage_provider", provider.id(), payload)))
                            .then());
                    });
            });
    }

    private String safe(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
