package run.ikaros.sync.api;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Minimal capability for checking whether a device may perform sync work.
 * Device persistence and trust-state vocabulary remain private to sync.
 */
public interface DeviceTrustQuery {
    Mono<Boolean> isUsable(UUID userId, UUID deviceId);
}
