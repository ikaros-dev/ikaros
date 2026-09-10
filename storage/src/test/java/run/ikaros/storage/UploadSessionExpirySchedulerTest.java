package run.ikaros.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.storage.api.StorageTier;
import run.ikaros.storage.api.UploadSessionState;

class UploadSessionExpirySchedulerTest {
    @Test
    void expiresSessionCleansObjectAndPublishesEvent() {
        UploadSessionRepository sessions = mock(UploadSessionRepository.class);
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        StorageObjectProviderRegistry objects = mock(StorageObjectProviderRegistry.class);
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        UploadSessionEntity open = session(id, UploadSessionState.OPEN, now.minusSeconds(1));
        UploadSessionEntity expired = session(id, UploadSessionState.EXPIRED, open.expiresAt());
        StorageProvider provider = new StorageProvider(UUID.randomUUID(), "local", "local", StorageTier.WARM,
            StorageProviderStatus.ENABLED, null, Map.of(), now, now);
        when(sessions.findAllByStateInAndExpiresAtBefore(any(), any())).thenReturn(Flux.just(open));
        when(sessions.save(any(UploadSessionEntity.class))).thenReturn(Mono.just(expired));
        when(providers.getByKey("local")).thenReturn(Mono.just(provider));
        when(objects.deleteObject(provider, "tmp/a.bin")).thenReturn(Mono.empty());
        when(events.append(any(EventAppendRequest.class))).thenReturn(Mono.empty());

        StepVerifier.create(new UploadSessionExpiryScheduler(sessions, providers, objects, events).expireNow())
            .verifyComplete();

        verify(sessions).save(any(UploadSessionEntity.class));
        verify(objects).deleteObject(provider, "tmp/a.bin");
        verify(events).append(any(EventAppendRequest.class));
    }

    @Test
    void keepsScanningExpiredSessionWhenCleanupFails() {
        UploadSessionRepository sessions = mock(UploadSessionRepository.class);
        StorageProviderRegistry providers = mock(StorageProviderRegistry.class);
        StorageObjectProviderRegistry objects = mock(StorageObjectProviderRegistry.class);
        DurableEventPublisher events = mock(DurableEventPublisher.class);
        UploadSessionEntity expired = session(UUID.randomUUID(), UploadSessionState.EXPIRED, Instant.now().minusSeconds(1));
        when(sessions.findAllByStateInAndExpiresAtBefore(any(), any())).thenReturn(Flux.just(expired));
        when(providers.getByKey("local")).thenReturn(Mono.error(new IllegalStateException("provider unavailable")));

        StepVerifier.create(new UploadSessionExpiryScheduler(sessions, providers, objects, events).expireNow())
            .verifyComplete();
        verify(events, org.mockito.Mockito.never()).append(any(EventAppendRequest.class));
    }

    private UploadSessionEntity session(UUID id, UploadSessionState state, Instant expiresAt) {
        Instant now = Instant.now();
        return new UploadSessionEntity(id, UUID.randomUUID(), UUID.randomUUID(), "local", "tmp/a.bin", 10L,
            "a".repeat(64), state, expiresAt, now, now, 0L, "key");
    }
}
