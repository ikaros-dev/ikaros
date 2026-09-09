package run.ikaros.reading;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PersistentReadingBookmarkServiceTest {
    @Test void createsListsAndDeletesOwnerBookmark() {
        UUID owner = UUID.randomUUID(); UUID workId = UUID.randomUUID(); UUID editionId = UUID.randomUUID(); UUID chapterId = UUID.randomUUID();
        ReadingBookmarkEntity bookmark = new ReadingBookmarkEntity(UUID.randomUUID(), owner, workId, editionId, chapterId, "EPUB_LOCATION", "OEBPS/one.xhtml", null, "第一章", Instant.now());
        ReadingBookmarkRepository bookmarks = org.mockito.Mockito.mock(ReadingBookmarkRepository.class);
        ReadingWorkRepository works = org.mockito.Mockito.mock(ReadingWorkRepository.class);
        ReadingEditionRepository editions = org.mockito.Mockito.mock(ReadingEditionRepository.class);
        ReadingChapterRepository chapters = org.mockito.Mockito.mock(ReadingChapterRepository.class);
        when(works.findById(workId)).thenReturn(Mono.just(new ReadingWorkEntity(workId, owner, UUID.randomUUID(), ReadingWorkKind.EBOOK.name(), "zh-CN", "READY", 0L)));
        when(editions.findById(editionId)).thenReturn(Mono.just(new ReadingEditionEntity(editionId, owner, workId, "Book", "zh-CN", null, null, 0, "AVAILABLE", 0L)));
        when(chapters.findById(chapterId)).thenReturn(Mono.just(new ReadingChapterEntity(chapterId, owner, workId, editionId, null, UUID.randomUUID(), "1", "第一章", "第一章", 0, "XHTML", null, null, "AVAILABLE", "ACTIVE", 0L)));
        when(bookmarks.save(any())).thenReturn(Mono.just(bookmark));
        when(bookmarks.findAllByOwnerIdAndWorkIdOrderByCreatedAtDesc(owner, workId)).thenReturn(Flux.just(bookmark));
        when(bookmarks.findById(bookmark.id())).thenReturn(Mono.just(bookmark));
        when(bookmarks.delete(bookmark)).thenReturn(Mono.empty());
        ReadingBookmarkService service = new PersistentReadingBookmarkService(bookmarks, works, editions, chapters);

        StepVerifier.create(service.create(owner, workId, editionId, chapterId, new CreateReadingBookmarkRequest("EPUB_LOCATION", "OEBPS/one.xhtml", null, "第一章")))
            .expectNextMatches(value -> value.id().equals(bookmark.id())).verifyComplete();
        StepVerifier.create(service.list(owner, workId)).expectNextCount(1).verifyComplete();
        StepVerifier.create(service.delete(owner, bookmark.id())).verifyComplete();
    }

    @Test void rejectsChapterFromAnotherEdition() {
        UUID owner = UUID.randomUUID(); UUID workId = UUID.randomUUID(); UUID editionId = UUID.randomUUID(); UUID chapterId = UUID.randomUUID();
        ReadingBookmarkRepository bookmarks = org.mockito.Mockito.mock(ReadingBookmarkRepository.class);
        ReadingWorkRepository works = org.mockito.Mockito.mock(ReadingWorkRepository.class);
        ReadingEditionRepository editions = org.mockito.Mockito.mock(ReadingEditionRepository.class);
        ReadingChapterRepository chapters = org.mockito.Mockito.mock(ReadingChapterRepository.class);
        when(works.findById(workId)).thenReturn(Mono.just(new ReadingWorkEntity(workId, owner, UUID.randomUUID(), ReadingWorkKind.EBOOK.name(), "zh-CN", "READY", 0L)));
        when(editions.findById(editionId)).thenReturn(Mono.empty());
        when(chapters.findById(chapterId)).thenReturn(Mono.empty());
        StepVerifier.create(new PersistentReadingBookmarkService(bookmarks, works, editions, chapters).create(owner, workId, editionId, chapterId, new CreateReadingBookmarkRequest("EPUB_LOCATION", "x", null, null)))
            .expectError(run.ikaros.common.NotFoundException.class).verify();
    }
}
