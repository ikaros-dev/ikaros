package run.ikaros.integration.api;

import reactor.core.publisher.Mono;

public interface DurableEventDeliveryDiagnostics {
    Mono<DurableEventDeliveryStatus> status();
}
