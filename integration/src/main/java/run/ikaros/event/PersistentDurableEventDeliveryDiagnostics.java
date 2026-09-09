package run.ikaros.event;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.integration.api.DurableEventDeliveryDiagnostics;
import run.ikaros.integration.api.DurableEventDeliveryStatus;

@Service
public class PersistentDurableEventDeliveryDiagnostics implements DurableEventDeliveryDiagnostics {
    private final OutboxEventRepository events;

    public PersistentDurableEventDeliveryDiagnostics(OutboxEventRepository events) {
        this.events = events;
    }

    @Override
    public Mono<DurableEventDeliveryStatus> status() {
        return Mono.zip(events.countPending(), events.countAttemptedPending())
            .flatMap(value -> events.lastAttemptAt()
                .map(last -> new DurableEventDeliveryStatus(value.getT1(), value.getT2(), last))
                .switchIfEmpty(Mono.just(new DurableEventDeliveryStatus(value.getT1(), value.getT2(), null))));
    }
}
