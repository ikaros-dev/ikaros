package run.ikaros.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
    private final DeliveryProviderRepository providers;
    private final ObjectMapper mapper;
    private final DurableEventService events;

    public PersistentDeliveryProviderService(DeliveryProviderRepository providers, ObjectMapper mapper,
        DurableEventService events) { this.providers = providers; this.mapper = mapper; this.events = events; }

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
                request.providerKey().trim(), request.providerType(), request.displayName().trim(), request.credentialRef(), config,
                "{}", DeliveryGrantRevocationLevel.IMMEDIATE, 1, DeliveryProviderHealthStatus.UNKNOWN,
                request.enabled() == null || request.enabled(), now, now, null, idempotencyKey)); }))
             .onErrorMap(DuplicateKeyException.class, e -> new ConflictException("Delivery Provider 标识已存在"))))
            .flatMap(saved -> emit("storage.delivery-provider.created", saved,
                "{\"delivery_provider_id\":\"" + saved.id() + "\",\"provider_type\":\"" + saved.providerType() + "\"}")
                .thenReturn(view(saved)));
    }

    @Override public Flux<DeliveryProviderView> list() { return providers.findAll().map(this::view); }

    @Override public Mono<DeliveryProviderView> get(UUID id) { return providers.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Delivery Provider 不存在"))).map(this::view); }

    @Override public Mono<DeliveryProviderView> update(UUID id, DeliveryProviderWriteRequest request, long expectedVersion) {
        validate(request);
        return encode(request.config()).flatMap(config -> providers.findById(id)
            .switchIfEmpty(Mono.error(new NotFoundException("Delivery Provider 不存在")))
            .flatMap(old -> { if ((old.version() == null ? 0 : old.version()) != expectedVersion)
                    return Mono.error(new PreconditionFailedException("If-Match 与 Delivery Provider 当前版本不匹配"));
                return providers.save(new DeliveryProviderEntity(old.id(), request.providerKey().trim(), request.providerType(),
                    request.displayName().trim(), request.credentialRef(), config, old.capabilities(), old.grantRevocationMode(),
                    old.signingKeyVersion(), old.healthStatus(), request.enabled() == null ? old.enabled() : request.enabled(),
                    old.createdAt(), Instant.now(), old.version())); })
            .flatMap(saved -> emit("storage.delivery-provider.updated", saved,
                "{\"delivery_provider_id\":\"" + saved.id() + "\",\"changed_fields\":[\"config\",\"enabled\"],\"version\":"
                    + (saved.version() == null ? 0 : saved.version()) + "}").thenReturn(view(saved))));
    }

    private void validate(DeliveryProviderWriteRequest request) {
        if (request.credentialRef() != null && !request.credentialRef().isBlank() && !request.credentialRef().startsWith("secret://"))
            throw new ConflictException("Delivery Provider credential_ref 必须使用 secret:// URI");
    }
    private Mono<String> encode(Map<String, Object> value) { try { return Mono.just(mapper.writeValueAsString(value == null ? Map.of() : value)); }
        catch (JsonProcessingException e) { return Mono.error(new IllegalArgumentException("Delivery Provider config 无法序列化", e)); } }
    private Mono<Void> emit(String type, DeliveryProviderEntity provider, String payload) {
        return events.append(type, 1, "delivery_provider", provider.id(), payload).then();
    }
    private DeliveryProviderView view(DeliveryProviderEntity e) { return new DeliveryProviderView(e.id(), e.providerKey(), e.providerType(), e.displayName(),
        e.credentialRef(), decode(e.config()), decode(e.capabilities()), e.grantRevocationMode(), e.signingKeyVersion(), e.healthStatus(), e.enabled(),
        e.createdAt(), e.updatedAt(), e.version() == null ? 0 : e.version()); }
    private Map<String, Object> decode(String value) { try { return mapper.readValue(value == null ? "{}" : value, new TypeReference<>() {}); }
        catch (JsonProcessingException e) { throw new ConflictException("Delivery Provider 配置数据损坏"); } }
}
