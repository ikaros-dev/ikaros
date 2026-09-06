package run.ikaros.storage;

import run.ikaros.storage.api.*;

import reactor.core.publisher.Mono;

/** Provider-specific health probe SPI; integrations may contribute an implementation. */
public interface DeliveryProviderProbe {
    boolean supports(DeliveryProviderEntity provider);
    Mono<DeliveryProviderHealthStatus> probe(DeliveryProviderEntity provider);
}
