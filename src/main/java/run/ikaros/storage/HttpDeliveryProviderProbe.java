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

    public HttpDeliveryProviderProbe(WebClient.Builder builder, ObjectMapper mapper) {
        this.client = builder.build();
        this.mapper = mapper;
    }

    @Override
    public boolean supports(DeliveryProviderEntity provider) {
        return endpoint(provider) != null;
    }

    @Override
    public Mono<DeliveryProviderHealthStatus> probe(DeliveryProviderEntity provider) {
        String endpoint = endpoint(provider);
        if (endpoint == null) return Mono.just(DeliveryProviderHealthStatus.UNKNOWN);
        return client.head()
            .uri(URI.create(endpoint))
            .header("User-Agent", "Ikaros-Delivery-Health-Check")
            .exchangeToMono(response -> Mono.just(classify(response.statusCode())))
            .timeout(TIMEOUT)
            .onErrorReturn(DeliveryProviderHealthStatus.UNHEALTHY);
    }

    private DeliveryProviderHealthStatus classify(HttpStatusCode status) {
        if (status.is2xxSuccessful() || status.is3xxRedirection() || status.value() == 401 || status.value() == 403) {
            return DeliveryProviderHealthStatus.HEALTHY;
        }
        if (status.is4xxClientError()) return DeliveryProviderHealthStatus.DEGRADED;
        return DeliveryProviderHealthStatus.UNHEALTHY;
    }

    private String endpoint(DeliveryProviderEntity provider) {
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
