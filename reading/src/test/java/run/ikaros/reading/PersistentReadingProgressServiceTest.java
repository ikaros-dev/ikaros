package run.ikaros.reading;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PersistentReadingProgressServiceTest {
    @Test void savesEpubLocatorAndProgressWithSessionVersion() {
        UUID owner = UUID.randomUUID(); UUID workId = UUID.randomUUID(); UUID editionId = UUID.randomUUID(); UUID chapterId = UUID.randomUUID(); UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        ReadingSessionEntity session = new ReadingSessionEntity(sessionId, owner, workId, editionId, chapterId, chapterId, "EPUB_LOCATION", "old.xhtml", "EPUB_LOCATION", "old.xhtml", now, now, null, false, 0L);
        ReadingSessionEntity updated = new ReadingSessionEntity(sessionId, owner, workId, editionId, chapterId, chapterId, "EPUB_LOCATION", "old.xhtml", "EPUB_LOCATION", "new.xhtml", now, now, null, false, 1L);
        ReadingProgressEntity progress = new ReadingProgressEntity(UUID.randomUUID(), owner, workId, editionId, chapterId, "EPUB_LOCATION", "new.xhtml", null, false, ReadingProgressIntent.NAVIGATE, sessionId, now, 0L);
        ReadingWorkRepository works = org.mockito.Mockito.mock(ReadingWorkRepository.class); ReadingEditionRepository editions = org.mockito.Mockito.mock(ReadingEditionRepository.class); ReadingChapterRepository chapters = org.mockito.Mockito.mock(ReadingChapterRepository.class); ReadingSessionRepository sessions = org.mockito.Mockito.mock(ReadingSessionRepository.class); ReadingProgressRepository progressRepository = org.mockito.Mockito.mock(ReadingProgressRepository.class); ReadingHistoryRepository history = org.mockito.Mockito.mock(ReadingHistoryRepository.class);
        when(sessions.findById(sessionId)).thenReturn(Mono.just(session)); when(sessions.save(any())).thenReturn(Mono.just(updated)); when(progressRepository.findByOwnerIdAndWorkIdAndEditionId(owner, workId, editionId)).thenReturn(Mono.empty()); when(progressRepository.save(any())).thenReturn(Mono.just(progress));

        StepVerifier.create(new PersistentReadingProgressService(works, editions, chapters, sessions, progressRepository, history).update(owner, sessionId, new UpdateReadingProgressRequest("EPUB_LOCATION", "new.xhtml", false, ReadingProgressIntent.NAVIGATE, null), 0L))
            .expectNextMatches(value -> value.locatorValue().equals("new.xhtml") && value.version() == 1L).verifyComplete();
    }

    @Test void returnsSavedProgressForOwnedWorkAndEdition() {
        UUID owner = UUID.randomUUID(); UUID workId = UUID.randomUUID(); UUID editionId = UUID.randomUUID();
        ReadingProgressEntity saved = new ReadingProgressEntity(UUID.randomUUID(), owner, workId, editionId, UUID.randomUUID(), "EPUB_LOCATION", "chapter.xhtml", null, false, ReadingProgressIntent.NAVIGATE, UUID.randomUUID(), Instant.now(), 2L);
        ReadingWorkRepository works = org.mockito.Mockito.mock(ReadingWorkRepository.class); ReadingEditionRepository editions = org.mockito.Mockito.mock(ReadingEditionRepository.class); ReadingChapterRepository chapters = org.mockito.Mockito.mock(ReadingChapterRepository.class); ReadingSessionRepository sessions = org.mockito.Mockito.mock(ReadingSessionRepository.class); ReadingProgressRepository progress = org.mockito.Mockito.mock(ReadingProgressRepository.class); ReadingHistoryRepository history = org.mockito.Mockito.mock(ReadingHistoryRepository.class);
        when(works.findById(workId)).thenReturn(Mono.just(new ReadingWorkEntity(workId, owner, UUID.randomUUID(), ReadingWorkKind.EBOOK.name(), "zh-CN", "READY", 0L)));
        when(progress.findByOwnerIdAndWorkIdAndEditionId(owner, workId, editionId)).thenReturn(Mono.just(saved));

        StepVerifier.create(new PersistentReadingProgressService(works, editions, chapters, sessions, progress, history).progress(owner, workId, editionId))
            .expectNextMatches(value -> value.chapterId().equals(saved.chapterId()) && value.locatorValue().equals("chapter.xhtml")).verifyComplete();
    }
}
