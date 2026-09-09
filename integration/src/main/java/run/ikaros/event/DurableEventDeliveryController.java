package run.ikaros.event;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.integration.api.DurableEvent;

/** Admin observability and retry entry point; event ownership remains in Integration. */
@RestController
@RequestMapping("/api/admin/integration/events")
public class DurableEventDeliveryController {
    private final OutboxDispatcher dispatcher;

    public DurableEventDeliveryController(OutboxDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @GetMapping
    public Flux<DurableEvent> listPending() {
        return dispatcher.pendingEvents();
    }

    @PostMapping("/{eventId}/actions/retry")
    public Mono<DurableEventRetryResult> retry(@PathVariable UUID eventId) {
        return dispatcher.retry(eventId).map(count -> new DurableEventRetryResult(eventId, count));
    }
}
