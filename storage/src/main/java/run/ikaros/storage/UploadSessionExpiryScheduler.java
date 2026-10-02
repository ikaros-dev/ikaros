package run.ikaros.storage;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.storage.api.UploadSessionState;

/** Marks expired upload sessions and removes only their temporary Provider objects. */
@Component
public class UploadSessionExpiryScheduler {
    private final UploadSessionRepository sessions;
    private final BlobPlacementRepository placements;
    private final StorageProviderRegistry providers;
    private final StorageObjectProviderRegistry objects;
    private final DurableEventPublisher events;
    private final AtomicBoolean running = new AtomicBoolean();

    public UploadSessionExpiryScheduler(UploadSessionRepository sessions, BlobPlacementRepository placements,
                                        StorageProviderRegistry providers,
                                        StorageObjectProviderRegistry objects, DurableEventPublisher events) {
        this.sessions = sessions;
        this.placements = placements;
        this.providers = providers;
        this.objects = objects;
        this.events = events;
    }

    @Scheduled(fixedDelayString = "${ikaros.storage.upload-session-expiry-scan-ms:30000}")
    public void expire() {
        if (!running.compareAndSet(false, true)) return;
        expireNow().onErrorResume(ignored -> Mono.empty())
            .doFinally(ignored -> running.set(false)).subscribe();
    }

    Mono<Void> expireNow() {
        Instant now = Instant.now();
        return sessions.findAllByStateInAndExpiresAtBefore(
                List.of(UploadSessionState.OPEN, UploadSessionState.RECEIVING, UploadSessionState.FINALIZING,
                    UploadSessionState.EXPIRED), now)
            .concatMap(session -> placements.findByProviderAndObjectKey(session.provider(), session.objectKey())
                .hasElement()
                .flatMap(hasPlacement -> hasPlacement
                    ? completeReferencedSession(session, now)
                    : expireAndClean(session, now))
                .onErrorResume(ignored -> Mono.empty()))
            .then();
    }

    private Mono<Void> completeReferencedSession(UploadSessionEntity session, Instant now) {
        if (session.state() == UploadSessionState.COMPLETED) return Mono.empty();
        return sessions.save(new UploadSessionEntity(session.id(), session.ownerId(), session.resourceId(),
            session.provider(), session.objectKey(), session.expectedSize(), session.declaredSha256(),
            UploadSessionState.COMPLETED, session.expiresAt(), session.createdAt(), now, session.version(),
            session.idempotencyKey())).then();
    }

    private Mono<Void> expireAndClean(UploadSessionEntity session, Instant now) {
        return expireState(session, now)
            .flatMap(expired -> cleanup(expired)
                .then(Mono.defer(() -> events.append(new EventAppendRequest("storage.upload-session.expired", 1,
                    "storage", "upload_session", expired.id(), "{\"session_id\":\"" + expired.id() + "\"}"))))
                .then());
    }

    private Mono<UploadSessionEntity> expireState(UploadSessionEntity session, Instant now) {
        if (session.state() == UploadSessionState.EXPIRED) return Mono.just(session);
        return sessions.save(new UploadSessionEntity(session.id(), session.ownerId(), session.resourceId(),
            session.provider(), session.objectKey(), session.expectedSize(), session.declaredSha256(),
            UploadSessionState.EXPIRED, session.expiresAt(), session.createdAt(), now, session.version(),
            session.idempotencyKey()));
    }

    private Mono<Void> cleanup(UploadSessionEntity session) {
        return providers.getByKey(session.provider())
            .switchIfEmpty(Mono.empty())
            .flatMap(provider -> objects.deleteObject(provider, session.objectKey()));
    }
}
