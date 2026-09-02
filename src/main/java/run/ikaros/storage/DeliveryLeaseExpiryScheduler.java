package run.ikaros.storage;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.event.DurableEventService;

/** 将已过期但仍未释放的 Lease 收敛为终态，并发出一次过期事件。 */
@Component
public class DeliveryLeaseExpiryScheduler {
    private final MediaDeliveryLeaseRepository leases;
    private final DurableEventService events;
    private final AtomicBoolean running = new AtomicBoolean();

    public DeliveryLeaseExpiryScheduler(MediaDeliveryLeaseRepository leases, DurableEventService events) {
        this.leases = leases;
        this.events = events;
    }

    @Scheduled(fixedDelayString = "${ikaros.delivery.lease-expiry-scan-ms:30000}")
    public void expire() {
        if (!running.compareAndSet(false, true)) return;
        Instant now = Instant.now();
        leases.findAllByReleasedAtIsNullAndLeaseExpiresAtBefore(now)
            .concatMap(lease -> leases.save(new MediaDeliveryLeaseEntity(lease.id(), lease.attachmentId(), lease.blobId(),
                lease.ownerId(), lease.grantId(), lease.bindingId(), lease.selectionEpoch(), lease.selectedAt(),
                lease.selectionReason(), lease.fallbackIndex(), lease.healthSnapshotVersion(), lease.leaseExpiresAt(),
                now, lease.lastHeartbeatAt(), lease.createdAt(), lease.version()))
                .flatMap(saved -> events.append("storage.delivery-lease.expired", 1, "delivery_lease", saved.id(),
                    "{\"lease_id\":\"" + saved.id() + "\",\"attachment_id\":\"" + saved.attachmentId() + "\"}").then()))
            .onErrorResume(ignored -> Mono.empty())
            .doFinally(ignored -> running.set(false))
            .subscribe();
    }
}
