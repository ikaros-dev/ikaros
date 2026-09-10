package run.ikaros.music;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class PersistentMusicPlaybackServiceTest {
  @Test
  void startsSessionForOwnedAvailableSource() {
    UUID owner = UUID.randomUUID();
    UUID trackId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    MusicTrackRepository tracks = org.mockito.Mockito.mock(MusicTrackRepository.class);
    MusicAudioSourceRepository sources = org.mockito.Mockito.mock(MusicAudioSourceRepository.class);
    MusicPlaybackSessionRepository sessions = org.mockito.Mockito.mock(MusicPlaybackSessionRepository.class);
    MusicPlaybackHistoryRepository history = org.mockito.Mockito.mock(MusicPlaybackHistoryRepository.class);
    MusicTrackEntity track = new MusicTrackEntity(trackId, owner, UUID.randomUUID(), "Song", 1000L, null, false, 0L);
    MusicAudioSourceEntity source = new MusicAudioSourceEntity(sourceId, owner, trackId, UUID.randomUUID(), "MP3", "MPEG", 1000L, null, null, null, null, false, "AVAILABLE", 0, 0L);
    MusicPlaybackSessionEntity saved = new MusicPlaybackSessionEntity(UUID.randomUUID(), owner, trackId, sourceId, null, MusicPlaybackState.ACTIVE, Instant.now(), Instant.now(), null, 0L, 0L);
    when(tracks.findById(trackId)).thenReturn(Mono.just(track));
    when(sources.findById(sourceId)).thenReturn(Mono.just(source));
    when(sessions.save(any())).thenReturn(Mono.just(saved));

    StepVerifier.create(new PersistentMusicPlaybackService(tracks, sources, sessions, history)
        .start(owner, trackId, new StartMusicPlaybackRequest(sourceId, null, 0L)))
        .expectNextMatches(view -> view.id().equals(saved.id())
            && view.trackId().equals(trackId) && view.sourceId().equals(sourceId)
            && view.state() == MusicPlaybackState.ACTIVE)
        .verifyComplete();
  }

  @Test
  void rejectsNegativePosition() {
    PersistentMusicPlaybackService service = new PersistentMusicPlaybackService(
        org.mockito.Mockito.mock(MusicTrackRepository.class),
        org.mockito.Mockito.mock(MusicAudioSourceRepository.class),
        org.mockito.Mockito.mock(MusicPlaybackSessionRepository.class),
        org.mockito.Mockito.mock(MusicPlaybackHistoryRepository.class));
    StepVerifier.create(service.start(UUID.randomUUID(), UUID.randomUUID(),
        new StartMusicPlaybackRequest(UUID.randomUUID(), null, -1L)))
        .expectError(IllegalArgumentException.class).verify();
  }

  @Test
  void rejectsSourceOwnedByAnotherUser() {
    UUID owner = UUID.randomUUID();
    UUID trackId = UUID.randomUUID();
    UUID sourceId = UUID.randomUUID();
    MusicTrackRepository tracks = org.mockito.Mockito.mock(MusicTrackRepository.class);
    MusicAudioSourceRepository sources = org.mockito.Mockito.mock(MusicAudioSourceRepository.class);
    MusicPlaybackSessionRepository sessions = org.mockito.Mockito.mock(MusicPlaybackSessionRepository.class);
    MusicTrackEntity track = new MusicTrackEntity(trackId, owner, UUID.randomUUID(), "Song", 1000L, null, false, 0L);
    MusicAudioSourceEntity source = new MusicAudioSourceEntity(sourceId, UUID.randomUUID(), trackId, UUID.randomUUID(), "MP3", "MPEG", 1000L, null, null, null, null, false, "AVAILABLE", 0, 0L);
    when(tracks.findById(trackId)).thenReturn(Mono.just(track));
    when(sources.findById(sourceId)).thenReturn(Mono.just(source));
    StepVerifier.create(new PersistentMusicPlaybackService(tracks, sources, sessions,
        org.mockito.Mockito.mock(MusicPlaybackHistoryRepository.class)).start(owner, trackId,
            new StartMusicPlaybackRequest(sourceId, null, 0L)))
        .expectError(run.ikaros.common.NotFoundException.class).verify();
  }

  @Test
  void listsOnlyActiveSessionsForResume() {
    UUID owner = UUID.randomUUID();
    UUID trackId = UUID.randomUUID();
    MusicPlaybackSessionRepository sessions = org.mockito.Mockito.mock(MusicPlaybackSessionRepository.class);
    MusicPlaybackSessionEntity session = new MusicPlaybackSessionEntity(UUID.randomUUID(), owner, trackId,
        UUID.randomUUID(), null, MusicPlaybackState.ACTIVE, Instant.now(), Instant.now(), null, 1200L, 0L);
    when(sessions.findAllByOwnerIdAndStateOrderByStartedAtDesc(owner, MusicPlaybackState.ACTIVE)).thenReturn(Flux.just(session));
    StepVerifier.create(new PersistentMusicPlaybackService(
        org.mockito.Mockito.mock(MusicTrackRepository.class), org.mockito.Mockito.mock(MusicAudioSourceRepository.class),
        sessions, org.mockito.Mockito.mock(MusicPlaybackHistoryRepository.class)).active(owner))
        .expectNextMatches(view -> view.id().equals(session.id()) && view.positionMillis() == 1200L)
        .verifyComplete();
  }
}
