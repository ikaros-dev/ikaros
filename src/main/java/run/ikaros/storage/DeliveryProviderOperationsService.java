package run.ikaros.storage;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.event.DurableEventService;
import run.ikaros.task.BackgroundTask;
import run.ikaros.task.BackgroundTaskService;

@Service
public class DeliveryProviderOperationsService {
    private final DeliveryProviderRepository providers;
    private final BackgroundTaskService tasks;
    private final DurableEventService events;

    public DeliveryProviderOperationsService(DeliveryProviderRepository providers, BackgroundTaskService tasks,
        DurableEventService events) { this.providers = providers; this.tasks = tasks; this.events = events; }

    public Mono<BackgroundTask> probe(UUID providerId, UUID actorId, String idempotencyKey) {
        return require(providerId).then(tasks.submit("storage.delivery-provider-probe",
            Map.of("provider_id", providerId.toString(), "requested_by", actorId.toString()),
            "storage.delivery-provider-probe:" + providerId + ":" + idempotencyKey))
            .flatMap(task -> events.append("storage.delivery-provider.probe-requested", 1, "delivery_provider", providerId,
                "{\"delivery_provider_id\":\"" + providerId + "\",\"task_id\":\"" + task.id() + "\"}").thenReturn(task));
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
            .flatMap(task -> events.append("storage.delivery-provider.signing-key-rotation-requested", 1, "delivery_provider", providerId,
                "{\"delivery_provider_id\":\"" + providerId + "\",\"task_id\":\"" + task.id() + "\"}").thenReturn(task));
    }

    private Mono<Void> require(UUID id) { return providers.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Delivery Provider 不存在"))).then(); }
}
