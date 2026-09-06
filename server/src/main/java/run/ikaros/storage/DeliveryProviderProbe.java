package run.ikaros.storage;

import reactor.core.publisher.Mono;

/** Provider-specific health probe SPI; integrations may contribute an implementation. */
public interface DeliveryProviderProbe {
    boolean supports(DeliveryProviderEntity provider);
    Mono<DeliveryProviderHealthStatus> probe(DeliveryProviderEntity provider);
}
