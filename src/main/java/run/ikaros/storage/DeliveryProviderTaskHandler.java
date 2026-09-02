package run.ikaros.storage;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.task.BackgroundTask;
import run.ikaros.task.BackgroundTaskDispatcher;
import run.ikaros.event.DurableEventService;

@Component
public class DeliveryProviderTaskHandler {
    private final BackgroundTaskDispatcher dispatcher;
    private final DeliveryProviderRepository providers;
    private final List<DeliveryProviderProbe> probes;
    private final DurableEventService events;

    public DeliveryProviderTaskHandler(BackgroundTaskDispatcher dispatcher, DeliveryProviderRepository providers,
        List<DeliveryProviderProbe> probes, DurableEventService events) {
        this.dispatcher = dispatcher; this.providers = providers; this.probes = probes; this.events = events;
    }

    @PostConstruct
    void register() {
        dispatcher.register("storage.delivery-provider-probe", this::probe);
        dispatcher.register("storage.delivery-provider-rotate-signing-key", this::rotate);
    }

    private Mono<Map<String, Object>> probe(BackgroundTask task) {
        UUID id = id(task);
        return providers.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Delivery Provider 不存在")))
            .flatMap(provider -> probes.stream().filter(p -> p.supports(provider)).findFirst()
                .map(p -> p.probe(provider)).orElseGet(() -> Mono.just(DeliveryProviderHealthStatus.UNKNOWN))
                .flatMap(status -> providers.save(new DeliveryProviderEntity(provider.id(), provider.providerKey(), provider.providerType(),
                    provider.displayName(), provider.credentialRef(), provider.config(), provider.capabilities(), provider.grantRevocationMode(),
                    provider.signingKeyVersion(), status, provider.enabled(), provider.createdAt(), Instant.now(), provider.version(), provider.idempotencyKey())))
                .flatMap(saved -> events.append("storage.delivery-provider.probed", 1, "delivery_provider", saved.id(),
                    "{\"delivery_provider_id\":\"" + saved.id() + "\",\"health_status\":\"" + saved.healthStatus()
                        + "\",\"capability_changes\":[]}").then(saved.healthStatus() == DeliveryProviderHealthStatus.DEGRADED
                    ? events.append("storage.delivery-provider.degraded", 1, "delivery_provider", saved.id(),
                        "{\"delivery_provider_id\":\"" + saved.id() + "\",\"reason_code\":\"health-check-degraded\"}").thenReturn(saved)
                    : Mono.just(saved)))
                .map(saved -> { Map<String, Object> result = new HashMap<>(); result.put("provider_id", saved.id().toString());
                    result.put("health_status", saved.healthStatus().name()); return result; }));
    }

    private Mono<Map<String, Object>> rotate(BackgroundTask task) {
        UUID id = id(task);
        String credentialRef = String.valueOf(task.payload().getOrDefault("credential_ref", ""));
        return providers.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Delivery Provider 不存在")))
            .flatMap(provider -> providers.save(new DeliveryProviderEntity(provider.id(), provider.providerKey(), provider.providerType(),
                provider.displayName(), credentialRef.isBlank() ? provider.credentialRef() : credentialRef, provider.config(), provider.capabilities(),
                provider.grantRevocationMode(), provider.signingKeyVersion() + 1, provider.healthStatus(), provider.enabled(), provider.createdAt(),
                Instant.now(), provider.version(), provider.idempotencyKey())))
            .flatMap(saved -> events.append("storage.delivery-provider.signing-key-rotated", 1, "delivery_provider", saved.id(),
                "{\"delivery_provider_id\":\"" + saved.id() + "\",\"previous_key_version\":"
                    + (saved.signingKeyVersion() - 1) + ",\"new_key_version\":" + saved.signingKeyVersion() + "}").thenReturn(saved))
            .map(saved -> { Map<String, Object> result = new HashMap<>(); result.put("provider_id", saved.id().toString());
                result.put("signing_key_version", saved.signingKeyVersion()); return result; });
    }

    private UUID id(BackgroundTask task) { return UUID.fromString(String.valueOf(task.payload().get("provider_id"))); }
}
