package run.ikaros.music;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PersistentMusicQueueServiceTest {
  @Test
  void appendsOwnedTrackAfterExistingEntries() {
    UUID owner = UUID.randomUUID();
    UUID queueId = UUID.randomUUID();
    UUID trackId = UUID.randomUUID();
    MusicQueueRepository queues = org.mockito.Mockito.mock(MusicQueueRepository.class);
    MusicQueueEntryRepository entries = org.mockito.Mockito.mock(MusicQueueEntryRepository.class);
    MusicTrackRepository tracks = org.mockito.Mockito.mock(MusicTrackRepository.class);
    when(queues.findById(queueId)).thenReturn(Mono.just(new MusicQueueEntity(queueId, owner, false,
        null, MusicRepeatMode.OFF, null, Instant.now(), Instant.now(), 0L)));
    when(tracks.findById(trackId)).thenReturn(Mono.just(new MusicTrackEntity(trackId, owner,
        UUID.randomUUID(), "Song", 1000L, null, false, 0L)));
    when(entries.findAllByQueueIdOrderByActivePositionAsc(queueId)).thenReturn(Flux.just(
        new MusicQueueEntryEntity(UUID.randomUUID(), queueId, UUID.randomUUID(), 0, 0, owner, Instant.now())));
    MusicQueueEntryEntity saved = new MusicQueueEntryEntity(UUID.randomUUID(), queueId, trackId,
        1, 1, owner, Instant.now());
    when(entries.save(any())).thenReturn(Mono.just(saved));

    StepVerifier.create(new PersistentMusicQueueService(queues, entries, tracks)
        .add(owner, queueId, new AddMusicQueueEntryRequest(trackId)))
        .expectNextMatches(view -> view.id().equals(saved.id()) && view.trackId().equals(trackId)
            && view.activePosition() == 1)
        .verifyComplete();
  }

  @Test
  void rejectsTrackOwnedByAnotherUser() {
    UUID owner = UUID.randomUUID();
    UUID queueId = UUID.randomUUID();
    UUID trackId = UUID.randomUUID();
    MusicQueueRepository queues = org.mockito.Mockito.mock(MusicQueueRepository.class);
    MusicQueueEntryRepository entries = org.mockito.Mockito.mock(MusicQueueEntryRepository.class);
    MusicTrackRepository tracks = org.mockito.Mockito.mock(MusicTrackRepository.class);
    when(queues.findById(queueId)).thenReturn(Mono.just(new MusicQueueEntity(queueId, owner, false,
        null, MusicRepeatMode.OFF, null, Instant.now(), Instant.now(), 0L)));
    when(tracks.findById(trackId)).thenReturn(Mono.just(new MusicTrackEntity(trackId,
        UUID.randomUUID(), UUID.randomUUID(), "Song", 1000L, null, false, 0L)));

    StepVerifier.create(new PersistentMusicQueueService(queues, entries, tracks)
        .add(owner, queueId, new AddMusicQueueEntryRequest(trackId)))
        .expectError(run.ikaros.common.NotFoundException.class).verify();
  }
}
