package run.ikaros.storage;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Performs a bounded reachability check against the configured public delivery endpoint. */
@Component
public class HttpDeliveryProviderProbe implements DeliveryProviderProbe {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private final WebClient client;
    private final ObjectMapper mapper;
    private final MediaDeliveryBindingRepository bindings;
    private final StorageProviderRegistry storageProviders;
    private final BlobPlacementRepository placements;
    private final StorageObjectProviderRegistry storageObjects;

    public HttpDeliveryProviderProbe(ObjectMapper mapper, MediaDeliveryBindingRepository bindings,
        StorageProviderRegistry storageProviders, BlobPlacementRepository placements,
        StorageObjectProviderRegistry storageObjects) {
        this.client = WebClient.builder().build();
        this.mapper = mapper;
        this.bindings = bindings;
        this.storageProviders = storageProviders;
        this.placements = placements;
        this.storageObjects = storageObjects;
    }

    @Override
    public boolean supports(DeliveryProviderEntity provider) {
        return true;
    }

    @Override
    public Mono<DeliveryProviderHealthStatus> probe(DeliveryProviderEntity provider) {
        if (provider.providerType() == DeliveryProviderType.DIRECT) {
            return directUrl(provider).flatMap(this::checkStorageRead)
                .switchIfEmpty(Mono.just(DeliveryProviderHealthStatus.UNKNOWN))
                .onErrorReturn(DeliveryProviderHealthStatus.UNHEALTHY);
        }
        String endpoint = endpoint(provider);
        if (endpoint == null) return Mono.just(DeliveryProviderHealthStatus.UNKNOWN);
        return provider.providerType() == DeliveryProviderType.SERVER_PROXY
            ? checkServerProxy(endpoint) : check(endpoint);
    }

    private Mono<DeliveryProviderHealthStatus> check(String endpoint) {
        return client.head()
            .uri(URI.create(endpoint))
            .header("User-Agent", "Ikaros-Delivery-Health-Check")
            .exchangeToMono(response -> Mono.just(classify(response.statusCode())))
            .timeout(TIMEOUT)
            .onErrorReturn(DeliveryProviderHealthStatus.UNHEALTHY);
    }

    private Mono<DeliveryProviderHealthStatus> checkStorageRead(String endpoint) {
        return client.get()
            .uri(URI.create(endpoint))
            .header("Range", "bytes=0-0")
            .header("User-Agent", "Ikaros-Delivery-Health-Check")
            .exchangeToMono(response -> Mono.just(classify(response.statusCode())))
            .timeout(TIMEOUT)
            .onErrorReturn(DeliveryProviderHealthStatus.UNHEALTHY);
    }

    private Mono<DeliveryProviderHealthStatus> checkServerProxy(String endpoint) {
        String healthEndpoint = endpoint.replaceAll("/+\\z", "") + "/api/health/ready";
        return client.get()
            .uri(URI.create(healthEndpoint))
            .header("User-Agent", "Ikaros-Delivery-Health-Check")
            .exchangeToMono(response -> Mono.just(classify(response.statusCode())))
            .timeout(TIMEOUT)
            .onErrorReturn(DeliveryProviderHealthStatus.UNHEALTHY);
    }

    private Mono<String> directUrl(DeliveryProviderEntity provider) {
        return bindings.findAllByDeliveryProviderKeyAndEnabledTrueOrderByPriorityAsc(provider.providerKey())
            .next()
            .flatMap(binding -> storageProviders.get(binding.storageProviderId()))
            .flatMap(storageProvider -> placements.findFirstByProviderAndPlacementState(storageProvider.providerKey(), PlacementState.ACTIVE)
                .flatMap(placement -> storageObjects.createReadIntent(storageProvider, placement.objectKey())))
            .map(StorageReadIntent::url)
            .switchIfEmpty(Mono.empty());
    }

    private DeliveryProviderHealthStatus classify(HttpStatusCode status) {
        if (status.is2xxSuccessful() || status.is3xxRedirection() || status.value() == 401 || status.value() == 403) {
            return DeliveryProviderHealthStatus.HEALTHY;
        }
        if (status.is4xxClientError()) return DeliveryProviderHealthStatus.DEGRADED;
        return DeliveryProviderHealthStatus.UNHEALTHY;
    }

    private String endpoint(DeliveryProviderEntity provider) {
        if (provider.providerType() == DeliveryProviderType.DIRECT) return null;
        try {
            Map<String, Object> config = mapper.readValue(provider.config() == null ? "{}" : provider.config().asString(),
                new TypeReference<>() { });
            Object value = config.get("endpoint");
            if (value == null || value.toString().isBlank()) return null;
            URI uri = URI.create(value.toString().trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) return null;
            return uri.toString();
        } catch (IllegalArgumentException | JacksonException ignored) {
            return null;
        }
    }
}
