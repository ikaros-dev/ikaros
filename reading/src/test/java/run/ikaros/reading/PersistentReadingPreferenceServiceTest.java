package run.ikaros.reading;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PersistentReadingPreferenceServiceTest {
    @Test void savesAndReadsEbookWorkPreferenceForOwner() {
        UUID owner = UUID.randomUUID();
        UUID workId = UUID.randomUUID();
        ReadingWorkEntity work = new ReadingWorkEntity(workId, owner, UUID.randomUUID(), ReadingWorkKind.EBOOK.name(), "zh-CN", "READY", 0L);
        ReadingPreferenceRepository repository = org.mockito.Mockito.mock(ReadingPreferenceRepository.class);
        ReadingWorkRepository works = org.mockito.Mockito.mock(ReadingWorkRepository.class);
        ReadingPreferenceEntity saved = new ReadingPreferenceEntity(UUID.randomUUID(), owner, ReadingPreferenceScope.WORK, "EBOOK", workId, "{\"fontSize\":20}", java.time.Instant.now(), 0L);
        when(works.findById(workId)).thenReturn(Mono.just(work));
        when(repository.findByOwnerIdAndScopeAndReadingKindAndWorkId(owner, ReadingPreferenceScope.WORK, "EBOOK", workId)).thenReturn(Mono.empty(), Mono.just(saved));
        when(repository.save(any())).thenReturn(Mono.just(saved));

        ReadingPreferenceService service = new PersistentReadingPreferenceService(repository, works);
        StepVerifier.create(service.update(owner, ReadingPreferenceScope.WORK, "EBOOK", workId, new UpdateReadingPreferenceRequest("{\"fontSize\":20}")))
            .expectNextMatches(value -> value.scope() == ReadingPreferenceScope.WORK && "EBOOK".equals(value.readingKind()))
            .verifyComplete();
        StepVerifier.create(service.get(owner, ReadingPreferenceScope.WORK, "EBOOK", workId))
            .expectNextMatches(value -> value.settings().contains("fontSize"))
            .verifyComplete();
    }

    @Test void rejectsPreferenceForAnotherOwnersWork() {
        UUID owner = UUID.randomUUID();
        UUID workId = UUID.randomUUID();
        ReadingPreferenceRepository repository = org.mockito.Mockito.mock(ReadingPreferenceRepository.class);
        ReadingWorkRepository works = org.mockito.Mockito.mock(ReadingWorkRepository.class);
        when(works.findById(workId)).thenReturn(Mono.just(new ReadingWorkEntity(workId, UUID.randomUUID(), UUID.randomUUID(), ReadingWorkKind.EBOOK.name(), "zh-CN", "READY", 0L)));
        when(repository.findByOwnerIdAndScopeAndReadingKindAndWorkId(owner, ReadingPreferenceScope.WORK, "EBOOK", workId)).thenReturn(Mono.empty());

        StepVerifier.create(new PersistentReadingPreferenceService(repository, works).update(owner, ReadingPreferenceScope.WORK, "EBOOK", workId, new UpdateReadingPreferenceRequest("{}")))
            .expectError(run.ikaros.common.NotFoundException.class)
            .verify();
    }
}
