package run.ikaros.music;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentMusicPlaybackService implements MusicPlaybackService {
  private final MusicTrackRepository tracks;
  private final MusicAudioSourceRepository sources;
  private final MusicPlaybackSessionRepository sessions;
  private final MusicPlaybackHistoryRepository history;
  public PersistentMusicPlaybackService(MusicTrackRepository tracks, MusicAudioSourceRepository sources, MusicPlaybackSessionRepository sessions, MusicPlaybackHistoryRepository history) { this.tracks = tracks; this.sources = sources; this.sessions = sessions; this.history = history; }
  public Mono<MusicPlaybackSessionView> start(UUID owner, UUID track, StartMusicPlaybackRequest request) { if (request.positionMillis() < 0) return Mono.error(new IllegalArgumentException("播放位置不能为负数")); return tracks.findById(track).filter(t -> t.ownerId().equals(owner)).switchIfEmpty(Mono.error(new NotFoundException("Track 不存在或无权访问"))).then(sources.findById(request.sourceId()).filter(s -> s.ownerId().equals(owner) && s.trackId().equals(track) && "AVAILABLE".equals(s.availability())).switchIfEmpty(Mono.error(new NotFoundException("Audio Source 不存在或不可用")))).flatMap(source -> { Instant now = Instant.now(); return sessions.save(new MusicPlaybackSessionEntity(null, owner, track, source.id(), request.queueId(), MusicPlaybackState.ACTIVE, now, now, null, request.positionMillis(), null)); }).map(this::view); }
  public Mono<MusicPlaybackSessionView> update(UUID owner, UUID id, UpdateMusicPlaybackRequest request) { return active(owner, id).flatMap(session -> sessions.save(new MusicPlaybackSessionEntity(session.id(), session.ownerId(), session.trackId(), session.sourceId(), session.queueId(), session.state(), session.startedAt(), Instant.now(), session.endedAt(), request.positionMillis(), session.version()))).map(this::view); }
  public Mono<MusicPlaybackSessionView> end(UUID owner, UUID id, EndMusicPlaybackRequest request) { return active(owner, id).flatMap(session -> { Instant now = Instant.now(); MusicPlaybackState outcome = request.outcome() == MusicPlaybackState.ACTIVE ? MusicPlaybackState.ABANDONED : request.outcome(); return sessions.save(new MusicPlaybackSessionEntity(session.id(), session.ownerId(), session.trackId(), session.sourceId(), session.queueId(), outcome, session.startedAt(), now, now, session.positionMillis(), session.version())).flatMap(done -> history.save(new MusicPlaybackHistoryEntity(null, owner, session.trackId(), session.sourceId(), session.startedAt(), now, Math.max(0, done.positionMillis()), outcome)).thenReturn(done)); }).map(this::view); }
  public Flux<MusicPlaybackHistoryView> history(UUID owner) { return history.findAllByOwnerIdOrderByEndedAtDesc(owner).take(100).map(this::historyView); }
  private Mono<MusicPlaybackSessionEntity> active(UUID owner, UUID id) { return sessions.findById(id).filter(s -> s.ownerId().equals(owner)).switchIfEmpty(Mono.error(new NotFoundException("Playback Session 不存在"))).flatMap(s -> s.state() == MusicPlaybackState.ACTIVE ? Mono.just(s) : Mono.error(new ConflictException("Playback Session 已结束"))); }
  private MusicPlaybackSessionView view(MusicPlaybackSessionEntity session) { return new MusicPlaybackSessionView(session.id(), session.trackId(), session.sourceId(), session.queueId(), session.state(), session.startedAt(), session.endedAt(), session.positionMillis()); }
  private MusicPlaybackHistoryView historyView(MusicPlaybackHistoryEntity item) { return new MusicPlaybackHistoryView(item.id(), item.trackId(), item.sourceId(), item.startedAt(), item.endedAt(), item.listenedMillis(), item.outcome()); }
}
