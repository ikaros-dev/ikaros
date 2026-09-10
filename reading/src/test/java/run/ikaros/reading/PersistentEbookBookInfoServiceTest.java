package run.ikaros.reading;

import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.api.ResourceClassification;
import run.ikaros.resource.api.ResourceLifecycle;
import run.ikaros.resource.api.ResourceService;
import run.ikaros.resource.api.ResourceType;
import run.ikaros.resource.api.ResourceView;

class PersistentEbookBookInfoServiceTest {
    @Test
    void returnsBookMetadataAndChapterCountForOwner() {
        UUID owner = UUID.randomUUID(), importId = UUID.randomUUID(), attachmentId = UUID.randomUUID();
        UUID workId = UUID.randomUUID(), resourceId = UUID.randomUUID(), editionId = UUID.randomUUID();
        Instant now = Instant.now();
        var imports = org.mockito.Mockito.mock(EbookImportRepository.class);
        var works = org.mockito.Mockito.mock(ReadingWorkRepository.class);
        var editions = org.mockito.Mockito.mock(ReadingEditionRepository.class);
        var chapters = org.mockito.Mockito.mock(ReadingChapterRepository.class);
        var resources = org.mockito.Mockito.mock(ResourceService.class);
        when(imports.findByIdAndOwnerId(importId, owner)).thenReturn(Mono.just(new EbookImportEntity(importId, owner,
            attachmentId, workId, editionId, ComicImportStatus.SUCCEEDED.name(), null, null, "key", now, now, 0L)));
        when(works.findById(workId)).thenReturn(Mono.just(new ReadingWorkEntity(workId, owner, resourceId,
            ReadingWorkKind.EBOOK.name(), "zh-CN", "ONGOING", 0L)));
        when(resources.get(owner, resourceId)).thenReturn(Mono.just(new ResourceView(resourceId, ResourceType.BOOK,
            "测试电子书", "简介", ResourceClassification.PRIVATE, ResourceLifecycle.ACTIVE, List.of(), List.of(), now, now, 0L)));
        when(editions.findById(editionId)).thenReturn(Mono.just(new ReadingEditionEntity(editionId, owner, workId,
            "EPUB", "zh-CN", "出版社", "book.epub", 0, "AVAILABLE", 0L)));
        when(chapters.findAllByOwnerIdAndEditionIdOrderBySortOrderAsc(owner, editionId)).thenReturn(Flux.just(
            new ReadingChapterEntity(UUID.randomUUID(), owner, workId, editionId, null, UUID.randomUUID(), null,
                "第一章", "第一章", 0, "XHTML", null, null, "AVAILABLE", "ACTIVE", 0L),
            new ReadingChapterEntity(UUID.randomUUID(), owner, workId, editionId, null, UUID.randomUUID(), null,
                "第二章", "第二章", 1, "XHTML", null, null, "AVAILABLE", "ACTIVE", 0L)));

        StepVerifier.create(new PersistentEbookBookInfoService(imports, works, editions, chapters, resources)
                .get(owner, importId))
            .expectNextMatches(info -> info.title().equals("测试电子书") && info.chapterCount() == 2
                && info.language().equals("zh-CN") && info.importStatus() == ComicImportStatus.SUCCEEDED)
            .verifyComplete();
    }
}
