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

    StepVerifier.create(new PersistentMusicQueueService(queues, entries, tracks,
        org.mockito.Mockito.mock(org.springframework.transaction.reactive.TransactionalOperator.class))
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

    StepVerifier.create(new PersistentMusicQueueService(queues, entries, tracks,
        org.mockito.Mockito.mock(org.springframework.transaction.reactive.TransactionalOperator.class))
        .add(owner, queueId, new AddMusicQueueEntryRequest(trackId)))
        .expectError(run.ikaros.common.NotFoundException.class).verify();
  }

  @Test
  void reordersEveryEntryAndChecksVersion() {
    UUID owner = UUID.randomUUID();
    UUID queueId = UUID.randomUUID();
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    MusicQueueRepository queues = org.mockito.Mockito.mock(MusicQueueRepository.class);
    MusicQueueEntryRepository entries = org.mockito.Mockito.mock(MusicQueueEntryRepository.class);
    MusicTrackRepository tracks = org.mockito.Mockito.mock(MusicTrackRepository.class);
    org.springframework.transaction.reactive.TransactionalOperator tx = org.mockito.Mockito.mock(
        org.springframework.transaction.reactive.TransactionalOperator.class);
    when(tx.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    MusicQueueEntity queue = new MusicQueueEntity(queueId, owner, false, null, MusicRepeatMode.OFF,
        null, Instant.now(), Instant.now(), 4L);
    MusicQueueEntryEntity firstEntry = new MusicQueueEntryEntity(first, queueId, UUID.randomUUID(),
        0, 0, owner, Instant.now());
    MusicQueueEntryEntity secondEntry = new MusicQueueEntryEntity(second, queueId, UUID.randomUUID(),
        1, 1, owner, Instant.now());
    when(queues.findById(queueId)).thenReturn(Mono.just(queue));
    when(entries.findAllByQueueIdOrderByActivePositionAsc(queueId)).thenReturn(Flux.just(firstEntry,
        secondEntry));
    when(entries.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(queues.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(new PersistentMusicQueueService(queues, entries, tracks, tx)
        .reorder(owner, queueId, java.util.List.of(second, first), 4L))
        .expectNextMatches(view -> view.id().equals(queueId)).verifyComplete();
  }

  @Test
  void updatesRepeatAndShufflePolicyWithVersion() {
    UUID owner = UUID.randomUUID();
    UUID queueId = UUID.randomUUID();
    MusicQueueRepository queues = org.mockito.Mockito.mock(MusicQueueRepository.class);
    MusicQueueEntryRepository entries = org.mockito.Mockito.mock(MusicQueueEntryRepository.class);
    MusicTrackRepository tracks = org.mockito.Mockito.mock(MusicTrackRepository.class);
    org.springframework.transaction.reactive.TransactionalOperator tx = org.mockito.Mockito.mock(
        org.springframework.transaction.reactive.TransactionalOperator.class);
    when(tx.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
    MusicQueueEntity queue = new MusicQueueEntity(queueId, owner, false, null, MusicRepeatMode.OFF,
        null, Instant.now(), Instant.now(), 2L);
    when(queues.findById(queueId)).thenReturn(Mono.just(queue));
    when(queues.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(new PersistentMusicQueueService(queues, entries, tracks, tx)
        .policy(owner, queueId, new UpdateMusicQueuePolicyRequest(MusicRepeatMode.QUEUE, true, 2L)))
        .expectNextMatches(view -> view.repeatMode() == MusicRepeatMode.QUEUE
            && view.shuffleEnabled()).verifyComplete();
  }
}
