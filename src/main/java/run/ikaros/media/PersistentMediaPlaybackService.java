package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PreconditionFailedException;
import run.ikaros.progress.ProgressType;
import run.ikaros.progress.ResourceProgressService;
import run.ikaros.progress.ResourceProgressView;
import run.ikaros.progress.SetProgressRequest;
import run.ikaros.resource.ResourceService;

@Service
public class PersistentMediaPlaybackService implements MediaPlaybackService {
    private final ResourceService resources;
    private final MediaReleaseRepository releases;
    private final MediaPlaybackSessionRepository sessions;
    private final MediaPlaybackHistoryRepository history;
    private final ResourceProgressService progress;

    public PersistentMediaPlaybackService(ResourceService resources, MediaReleaseRepository releases,
        MediaPlaybackSessionRepository sessions, MediaPlaybackHistoryRepository history, ResourceProgressService progress) {
        this.resources = resources; this.releases = releases; this.sessions = sessions; this.history = history; this.progress = progress;
    }

    @Override public Mono<PlaybackSessionView> start(UUID ownerId, UUID resourceId, StartPlaybackRequest request) {
        if (request.startPositionSeconds() < 0) return Mono.error(new IllegalArgumentException("播放位置不能为负数"));
        return resources.get(ownerId, resourceId).then(releases.findById(request.releaseId())
            .filter(r -> r.ownerId().equals(ownerId) && r.playableResourceId().equals(resourceId))
            .switchIfEmpty(Mono.error(new NotFoundException("Release 不存在或无权播放")))
            .flatMap(release -> {
                if (release.state() != MediaReleaseState.AVAILABLE) return Mono.error(new ConflictException("Release 当前不可播放"));
                Instant now = Instant.now();
                return sessions.save(new MediaPlaybackSessionEntity(null, ownerId, resourceId, release.id(), PlaybackSessionState.ACTIVE,
                    now, null, request.startPositionSeconds(), null));
            })).map(this::sessionView);
    }

    @Override public Mono<PlaybackSessionView> update(UUID ownerId, UUID sessionId, UpdatePlaybackProgressRequest request) {
        return ownedActive(ownerId, sessionId).flatMap(old -> {
            if (request.totalSeconds() != null && request.positionSeconds() > request.totalSeconds()) return Mono.error(new IllegalArgumentException("播放位置超过总时长"));
            return sessions.save(new MediaPlaybackSessionEntity(old.id(), old.ownerId(), old.resourceId(), old.releaseId(), old.state(), old.startedAt(), old.endedAt(), request.positionSeconds(), old.version()))
                .flatMap(updated -> progress.set(ownerId, old.resourceId(), new SetProgressRequest(ProgressType.VIDEO_SECONDS,
                    request.positionSeconds(), request.totalSeconds(), request.completed())).thenReturn(updated));
        }).map(this::sessionView);
    }

    @Override public Mono<PlaybackSessionView> update(UUID ownerId, UUID sessionId, UpdatePlaybackProgressRequest request,
                                                      long expectedVersion) {
        return ownedActive(ownerId, sessionId).flatMap(old -> {
            checkVersion(old.version(), expectedVersion);
            return update(ownerId, sessionId, request);
        });
    }

    @Override public Mono<PlaybackSessionView> end(UUID ownerId, UUID sessionId) {
        return ownedActive(ownerId, sessionId).flatMap(old -> {
            Instant now = Instant.now();
            return sessions.save(new MediaPlaybackSessionEntity(old.id(), old.ownerId(), old.resourceId(), old.releaseId(), PlaybackSessionState.ENDED,
                old.startedAt(), now, old.lastPositionSeconds(), old.version())).flatMap(updated -> history.save(new MediaPlaybackHistoryEntity(
                null, ownerId, old.resourceId(), old.id(), old.startedAt(), now, Math.max(0, updated.lastPositionSeconds()))).thenReturn(updated));
        }).map(this::sessionView);
    }

    @Override public Mono<PlaybackSessionView> end(UUID ownerId, UUID sessionId, long expectedVersion) {
        return ownedActive(ownerId, sessionId).flatMap(old -> {
            checkVersion(old.version(), expectedVersion);
            return end(ownerId, sessionId);
        });
    }

    @Override public Mono<ResourceProgressView> progress(UUID ownerId, UUID resourceId) { return progress.get(ownerId, resourceId, ProgressType.VIDEO_SECONDS); }
    @Override public Flux<PlaybackHistoryView> history(UUID ownerId) { return history.findAllByOwnerIdOrderByEndedAtDesc(ownerId).take(100).map(this::historyView); }
    private Mono<MediaPlaybackSessionEntity> ownedActive(UUID ownerId, UUID id) { return sessions.findById(id).filter(s -> s.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Playback Session 不存在"))).flatMap(s -> s.state() == PlaybackSessionState.ACTIVE ? Mono.just(s) : Mono.error(new ConflictException("Playback Session 已结束"))); }
    private void checkVersion(Long actual, long expected) { if ((actual == null ? 0 : actual) != expected) throw new PreconditionFailedException("If-Match 与 Playback Session 当前版本不匹配"); }
    private PlaybackSessionView sessionView(MediaPlaybackSessionEntity e) { return new PlaybackSessionView(e.id(), e.resourceId(), e.releaseId(), e.state(), e.startedAt(), e.endedAt(), e.lastPositionSeconds(), e.version()); }
    private PlaybackHistoryView historyView(MediaPlaybackHistoryEntity e) { return new PlaybackHistoryView(e.id(), e.resourceId(), e.sessionId(), e.startedAt(), e.endedAt(), e.watchedSeconds()); }
}
