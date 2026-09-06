package run.ikaros.storage;

import run.ikaros.storage.api.*;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.operations.task.BackgroundTask;
import run.ikaros.operations.task.BackgroundTaskService;

@Service
public class DeliveryProviderOperationsService {
    static final UUID SYSTEM_ACTOR_ID = new UUID(0L, 0L);
    private final DeliveryProviderRepository providers;
    private final BackgroundTaskService tasks;
    private final DurableEventPublisher events;

    public DeliveryProviderOperationsService(DeliveryProviderRepository providers, BackgroundTaskService tasks,
        DurableEventPublisher events) { this.providers = providers; this.tasks = tasks; this.events = events; }

    public Mono<BackgroundTask> probe(UUID providerId, UUID actorId, String idempotencyKey) {
        UUID requestedBy = actorId == null ? SYSTEM_ACTOR_ID : actorId;
        return require(providerId).then(tasks.submit("storage.delivery-provider-probe",
            Map.of("provider_id", providerId.toString(), "requested_by", requestedBy.toString()),
            "storage.delivery-provider-probe:" + providerId + ":" + idempotencyKey))
            .flatMap(task -> events.append(new EventAppendRequest("storage.delivery-provider.probe-requested", 1, "storage", "delivery_provider", providerId,
                "{\"delivery_provider_id\":\"" + providerId + "\",\"task_id\":\"" + task.id() + "\"}")).thenReturn(task));
    }

    public Mono<BackgroundTask> rotate(UUID providerId, UUID actorId, String idempotencyKey,
        RotateDeliverySigningKeyRequest request) {
        if (request != null && request.credentialRef() != null && !request.credentialRef().isBlank()
            && !request.credentialRef().startsWith("secret://")) {
            return Mono.error(new ConflictException("credential_ref 必须使用 secret:// URI"));
        }
        Map<String, Object> payload = Map.of("provider_id", providerId.toString(), "requested_by", actorId.toString(),
            "credential_ref", request == null || request.credentialRef() == null ? "" : request.credentialRef(),
            "emergency", request != null && Boolean.TRUE.equals(request.emergency()));
        return require(providerId).then(tasks.submit("storage.delivery-provider-rotate-signing-key", payload,
            "storage.delivery-provider-rotate-signing-key:" + providerId + ":" + idempotencyKey))
            .flatMap(task -> events.append(new EventAppendRequest("storage.delivery-provider.signing-key-rotation-requested", 1, "storage", "delivery_provider", providerId,
                "{\"delivery_provider_id\":\"" + providerId + "\",\"task_id\":\"" + task.id() + "\"}")).thenReturn(task));
    }

    private Mono<Void> require(UUID id) { return providers.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Delivery Provider 不存在"))).then(); }
}
