package run.ikaros.operations.health;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Opens readiness only after the complete application startup sequence succeeds. */
@Component
public final class ApplicationReadiness {
    private volatile boolean ready;

    @EventListener(ApplicationReadyEvent.class)
    public void markReady() {
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }
}
