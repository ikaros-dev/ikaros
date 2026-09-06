package run.ikaros.storage;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PreconditionFailedException;
import run.ikaros.event.DurableEventService;

@Service
public class PersistentDeliveryProviderService implements DeliveryProviderService {
    private static final int MAX_UNPAGED_RESULTS = 100;
    private final DeliveryProviderRepository providers;
    private final ObjectMapper mapper;
    private final DurableEventService events;
    private final DeliveryProviderOperationsService operations;
    private final MediaDeliveryBindingRepository bindings;

    public PersistentDeliveryProviderService(DeliveryProviderRepository providers, ObjectMapper mapper,
        DurableEventService events, DeliveryProviderOperationsService operations, MediaDeliveryBindingRepository bindings) {
        this.providers = providers; this.mapper = mapper; this.events = events; this.operations = operations; this.bindings = bindings;
    }

    @Override public Mono<DeliveryProviderView> enable(UUID id) { return changeEnabled(id, true); }

    @Override public Mono<DeliveryProviderView> disable(UUID id) { return changeEnabled(id, false); }

    private Mono<DeliveryProviderView> changeEnabled(UUID id, boolean enabled) {
        return providers.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Delivery Provider 不存在")))
            .flatMap(old -> {
                if (old.enabled() == enabled) return Mono.just(view(old));
                DeliveryProviderEntity replacement = replacement(old, enabled);
                return providers.save(replacement)
                    .flatMap(saved -> emit(enabled ? "storage.delivery-provider.enabled" : "storage.delivery-provider.disabled", saved,
                        "{\"delivery_provider_id\":\"" + saved.id() + "\"}").thenReturn(view(saved)));
            });
    }

    @Override public Mono<Void> delete(UUID id) {
        return providers.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Delivery Provider 不存在")))
            .flatMap(provider -> bindings.existsByDeliveryProviderKey(provider.providerKey()).flatMap(referenced -> {
                if (referenced) return Mono.error(new ConflictException("Delivery Provider 仍被 Delivery Binding 引用，请先删除或停用相关 Binding"));
                return providers.delete(provider).then(emit("storage.delivery-provider.removed", provider,
                    "{\"delivery_provider_id\":\"" + provider.id() + "\"}"));
            }));
    }

    private DeliveryProviderEntity replacement(DeliveryProviderEntity old, boolean enabled) {
        return new DeliveryProviderEntity(old.id(), old.providerKey(), old.providerType(), old.displayName(), old.credentialRef(),
            old.config(), old.capabilities(), old.grantRevocationMode(), old.signingKeyVersion(), old.healthStatus(), enabled,
            old.createdAt(), Instant.now(), old.version(), old.idempotencyKey());
    }

    @Override public Mono<DeliveryProviderView> create(DeliveryProviderWriteRequest request) {
        return create(request, null);
    }

    @Override public Mono<DeliveryProviderView> create(DeliveryProviderWriteRequest request, String idempotencyKey) {
        validate(request);
        Mono<DeliveryProviderEntity> byIdempotency = idempotencyKey == null || idempotencyKey.isBlank()
            ? Mono.empty() : providers.findByIdempotencyKey(idempotencyKey);
        return encode(request.config()).flatMap(config -> byIdempotency
            .switchIfEmpty(providers.findByProviderKey(request.providerKey().trim())
            .flatMap(old -> Mono.<DeliveryProviderEntity>error(new ConflictException("Delivery Provider 标识已存在")))
            .switchIfEmpty(Mono.defer(() -> { Instant now = Instant.now(); return providers.save(new DeliveryProviderEntity(null,
                request.providerKey().trim(), request.providerType(), request.displayName().trim(), request.credentialRef(), Json.of(config),
                Json.of("{}"), DeliveryGrantRevocationLevel.IMMEDIATE, 1, DeliveryProviderHealthStatus.UNKNOWN,
                request.enabled() == null || request.enabled(), now, now, null, idempotencyKey)); }))
             .onErrorMap(DuplicateKeyException.class, e -> new ConflictException("Delivery Provider 标识已存在"))))
            .flatMap(saved -> emit("storage.delivery-provider.created", saved,
                "{\"delivery_provider_id\":\"" + saved.id() + "\",\"provider_type\":\"" + saved.providerType() + "\"}")
                .then(operations.probe(saved.id(), DeliveryProviderOperationsService.SYSTEM_ACTOR_ID,
                    "auto-create:" + saved.id()))
                .thenReturn(view(saved)));
    }

    @Override public Flux<DeliveryProviderView> list() { return providers.findAll().take(MAX_UNPAGED_RESULTS).map(this::view); }

    @Override public Mono<DeliveryProviderView> get(UUID id) { return providers.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Delivery Provider 不存在"))).map(this::view); }

    @Override public Mono<DeliveryProviderView> update(UUID id, DeliveryProviderWriteRequest request, long expectedVersion) {
        validate(request);
        return encode(request.config()).flatMap(config -> providers.findById(id)
            .switchIfEmpty(Mono.error(new NotFoundException("Delivery Provider 不存在")))
            .flatMap(old -> {
                long actualVersion = old.version() == null ? 0 : old.version();
                if (actualVersion != expectedVersion) {
                    return Mono.error(new PreconditionFailedException("If-Match 与 Delivery Provider 当前版本不匹配"));
                }
                DeliveryProviderEntity replacement = new DeliveryProviderEntity(
                    old.id(), request.providerKey().trim(), request.providerType(), request.displayName().trim(),
                    request.credentialRef(), Json.of(config), old.capabilities(), old.grantRevocationMode(), old.signingKeyVersion(),
                    old.healthStatus(), request.enabled() == null ? old.enabled() : request.enabled(), old.createdAt(),
                    Instant.now(), old.version(), old.idempotencyKey());
                return providers.save(replacement)
                    .onErrorMap(DuplicateKeyException.class, error -> new ConflictException("Delivery Provider 标识已存在"))
                    .flatMap(saved -> {
                    List<String> changedFields = changedFields(old, saved);
                    Mono<Void> stateEvent = old.enabled() == saved.enabled() ? Mono.<Void>empty()
                        : emit(saved.enabled() ? "storage.delivery-provider.enabled" : "storage.delivery-provider.disabled", saved,
                            "{\"delivery_provider_id\":\"" + saved.id() + "\"}");
                    return emit("storage.delivery-provider.updated", saved,
                        "{\"delivery_provider_id\":\"" + saved.id() + "\",\"changed_fields\":" + jsonArray(changedFields) + ",\"version\":"
                            + (saved.version() == null ? 0 : saved.version()) + "}")
                        .then(stateEvent).thenReturn(view(saved));
                });
            }));
    }

    private void validate(DeliveryProviderWriteRequest request) {
        if (request.credentialRef() != null && !request.credentialRef().isBlank() && !request.credentialRef().startsWith("secret://"))
            throw new ConflictException("Delivery Provider credential_ref 必须使用 secret:// URI");
    }
    private Mono<String> encode(Map<String, Object> value) { try { return Mono.just(mapper.writeValueAsString(value == null ? Map.of() : value)); }
        catch (JacksonException e) { return Mono.error(new IllegalArgumentException("Delivery Provider config 无法序列化", e)); } }
    private Mono<Void> emit(String type, DeliveryProviderEntity provider, String payload) {
        return events.append(type, 1, "delivery_provider", provider.id(), payload).then();
    }
    private List<String> changedFields(DeliveryProviderEntity old, DeliveryProviderEntity saved) {
        List<String> fields = new ArrayList<>();
        if (!old.providerKey().equals(saved.providerKey())) fields.add("provider_key");
        if (old.providerType() != saved.providerType()) fields.add("provider_type");
        if (!old.displayName().equals(saved.displayName())) fields.add("display_name");
        if (!java.util.Objects.equals(old.credentialRef(), saved.credentialRef())) fields.add("credential_ref");
        if (!old.config().equals(saved.config())) fields.add("config");
        if (old.enabled() != saved.enabled()) fields.add("enabled");
        return fields;
    }
    private String jsonArray(List<String> fields) {
        return fields.stream().map(field -> "\"" + field + "\"").collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
    private record ProviderChange(DeliveryProviderEntity old, DeliveryProviderEntity saved) {}
    private DeliveryProviderView view(DeliveryProviderEntity e) { return new DeliveryProviderView(e.id(), e.providerKey(), e.providerType(), e.displayName(),
        e.credentialRef(), decode(e.config()), decode(e.capabilities()), e.grantRevocationMode(), e.signingKeyVersion(), e.healthStatus(), e.enabled(),
        e.createdAt(), e.updatedAt(), e.version() == null ? 0 : e.version()); }
    private Map<String, Object> decode(Json value) { try { return mapper.readValue(value == null ? "{}" : value.asString(), new TypeReference<>() {}); }
        catch (JacksonException e) { throw new ConflictException("Delivery Provider 配置数据损坏"); } }
}
