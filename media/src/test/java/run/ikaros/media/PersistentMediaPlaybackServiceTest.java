package run.ikaros.media;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.api.ResourceClassification;
import run.ikaros.resource.api.ResourceLifecycle;
import run.ikaros.resource.api.ResourceProgressService;
import run.ikaros.resource.api.ResourceService;
import run.ikaros.resource.api.ResourceType;
import run.ikaros.resource.api.ResourceView;

class PersistentMediaPlaybackServiceTest {
    @Test
    void startsSessionForOwnedAvailableRelease() {
        UUID owner = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        ResourceService resources = org.mockito.Mockito.mock(ResourceService.class);
        MediaReleaseRepository releases = org.mockito.Mockito.mock(MediaReleaseRepository.class);
        MediaPlaybackSessionRepository sessions = org.mockito.Mockito.mock(MediaPlaybackSessionRepository.class);
        MediaPlaybackHistoryRepository history = org.mockito.Mockito.mock(MediaPlaybackHistoryRepository.class);
        ResourceProgressService progress = org.mockito.Mockito.mock(ResourceProgressService.class);
        when(resources.get(owner, resourceId)).thenReturn(Mono.just(resource(resourceId)));
        when(releases.findById(releaseId)).thenReturn(Mono.just(release(owner, resourceId, releaseId, MediaReleaseState.AVAILABLE)));
        when(sessions.save(any(MediaPlaybackSessionEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(new PersistentMediaPlaybackService(resources, releases, sessions, history, progress)
                .start(owner, resourceId, new StartPlaybackRequest(releaseId, 42)))
            .expectNextMatches(view -> view.resourceId().equals(resourceId)
                && view.releaseId().equals(releaseId)
                && view.lastPositionSeconds() == 42
                && view.state() == PlaybackSessionState.ACTIVE)
            .verifyComplete();
        verify(sessions).save(any(MediaPlaybackSessionEntity.class));
    }

    @Test
    void rejectsUnavailableReleaseBeforeCreatingSession() {
        UUID owner = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        ResourceService resources = org.mockito.Mockito.mock(ResourceService.class);
        MediaReleaseRepository releases = org.mockito.Mockito.mock(MediaReleaseRepository.class);
        MediaPlaybackSessionRepository sessions = org.mockito.Mockito.mock(MediaPlaybackSessionRepository.class);
        MediaPlaybackHistoryRepository history = org.mockito.Mockito.mock(MediaPlaybackHistoryRepository.class);
        ResourceProgressService progress = org.mockito.Mockito.mock(ResourceProgressService.class);
        when(resources.get(owner, resourceId)).thenReturn(Mono.just(resource(resourceId)));
        when(releases.findById(releaseId)).thenReturn(Mono.just(release(owner, resourceId, releaseId, MediaReleaseState.ARCHIVED)));

        StepVerifier.create(new PersistentMediaPlaybackService(resources, releases, sessions, history, progress)
                .start(owner, resourceId, new StartPlaybackRequest(releaseId, 0)))
            .expectErrorMessage("Release 当前不可播放")
            .verify();
        verify(sessions, never()).save(any(MediaPlaybackSessionEntity.class));
    }

    @Test
    void persistsProgressAndUpdatesResourceProgress() {
        UUID owner = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceService resources = org.mockito.Mockito.mock(ResourceService.class);
        MediaReleaseRepository releases = org.mockito.Mockito.mock(MediaReleaseRepository.class);
        MediaPlaybackSessionRepository sessions = org.mockito.Mockito.mock(MediaPlaybackSessionRepository.class);
        MediaPlaybackHistoryRepository history = org.mockito.Mockito.mock(MediaPlaybackHistoryRepository.class);
        ResourceProgressService progress = org.mockito.Mockito.mock(ResourceProgressService.class);
        MediaPlaybackSessionEntity current = new MediaPlaybackSessionEntity(sessionId, owner, resourceId, UUID.randomUUID(),
            PlaybackSessionState.ACTIVE, now, null, 10, 0L);
        when(sessions.findById(sessionId)).thenReturn(Mono.just(current));
        when(sessions.save(any(MediaPlaybackSessionEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(progress.set(any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(new PersistentMediaPlaybackService(resources, releases, sessions, history, progress)
                .update(owner, sessionId, new UpdatePlaybackProgressRequest(35, 100L, false)))
            .expectNextMatches(view -> view.lastPositionSeconds() == 35 && view.id().equals(sessionId))
            .verifyComplete();
        verify(progress).set(any(), any(), any());
        verify(sessions).save(any(MediaPlaybackSessionEntity.class));
    }

    @Test
    void rejectsProgressBeyondKnownDurationWithoutWriting() {
        UUID owner = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        MediaPlaybackSessionRepository sessions = org.mockito.Mockito.mock(MediaPlaybackSessionRepository.class);
        MediaPlaybackSessionEntity current = new MediaPlaybackSessionEntity(sessionId, owner, resourceId, UUID.randomUUID(),
            PlaybackSessionState.ACTIVE, Instant.now(), null, 10, 0L);
        when(sessions.findById(sessionId)).thenReturn(Mono.just(current));

        StepVerifier.create(new PersistentMediaPlaybackService(
                org.mockito.Mockito.mock(ResourceService.class),
                org.mockito.Mockito.mock(MediaReleaseRepository.class),
                sessions,
                org.mockito.Mockito.mock(MediaPlaybackHistoryRepository.class),
                org.mockito.Mockito.mock(ResourceProgressService.class))
                .update(owner, sessionId, new UpdatePlaybackProgressRequest(101, 100L, false)))
            .expectErrorMessage("播放位置超过总时长")
            .verify();
        verify(sessions, never()).save(any(MediaPlaybackSessionEntity.class));
    }

    private static ResourceView resource(UUID id) {
        return new ResourceView(id, ResourceType.VIDEO, "视频", null, ResourceClassification.PRIVATE,
            ResourceLifecycle.ACTIVE, List.of(), List.of(), Instant.now(), Instant.now(), 0L);
    }

    private static MediaReleaseEntity release(UUID owner, UUID resourceId, UUID id, MediaReleaseState state) {
        Instant now = Instant.now();
        return new MediaReleaseEntity(id, owner, resourceId, UUID.randomUUID(), "WEB", "1080P", state,
            "sha256:test", now, now, 0L);
    }
}
