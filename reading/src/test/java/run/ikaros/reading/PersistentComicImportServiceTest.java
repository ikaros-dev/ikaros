package run.ikaros.reading;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.storage.api.AttachmentAvailabilityStatus;
import run.ikaros.storage.api.AttachmentKind;
import run.ikaros.storage.api.AttachmentView;
import run.ikaros.storage.api.StorageService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PersistentComicImportServiceTest {
    @Test
    void acceptsSupportedPackageAndPersistsImport() {
        UUID owner = UUID.randomUUID(); UUID attachmentId = UUID.randomUUID();
        UUID workId = UUID.randomUUID(); UUID editionId = UUID.randomUUID(); UUID importId = UUID.randomUUID();
        StorageService storage = org.mockito.Mockito.mock(StorageService.class);
        ReadingCatalogService catalog = org.mockito.Mockito.mock(ReadingCatalogService.class);
        ComicImportRepository imports = org.mockito.Mockito.mock(ComicImportRepository.class);
        when(imports.findByOwnerIdAndIdempotencyKey(owner, "key")).thenReturn(Mono.empty());
        when(storage.get(owner, attachmentId)).thenReturn(Mono.just(new AttachmentView(attachmentId, UUID.randomUUID(),
            "示例.cbz", AttachmentKind.ORIGINAL, "a".repeat(64), 10L, "application/zip", AttachmentAvailabilityStatus.READY)));
        when(catalog.createWork(any(), any())).thenReturn(Mono.just(new ReadingWorkView(workId, UUID.randomUUID(),
            ReadingWorkKind.COMIC, "zh-CN")));
        when(catalog.createEdition(any(), any(), any())).thenReturn(Mono.just(new ReadingEditionView(editionId,
            workId, "导入包", "zh-CN", null, "示例.cbz", 0, "AVAILABLE")));
        Instant now = Instant.now();
        when(imports.save(any())).thenReturn(Mono.just(new ComicImportEntity(importId, owner, attachmentId, workId,
            editionId, ComicImportStatus.ACCEPTED.name(), null, null, "key", now, now, 0L)));

        StepVerifier.create(new PersistentComicImportService(storage, catalog, imports)
                .create(owner, new CreateComicImportRequest(attachmentId, null, "zh-CN"), "key"))
            .expectNextMatches(view -> view.id().equals(importId) && view.status() == ComicImportStatus.ACCEPTED)
            .verifyComplete();
        verify(catalog).createWork(any(), any()); verify(catalog).createEdition(any(), any(), any());
        verify(imports).save(any(ComicImportEntity.class));
    }

    @Test
    void rejectsUnsupportedPackageBeforeCreatingReadingRecords() {
        UUID owner = UUID.randomUUID(); UUID attachmentId = UUID.randomUUID();
        StorageService storage = org.mockito.Mockito.mock(StorageService.class);
        ReadingCatalogService catalog = org.mockito.Mockito.mock(ReadingCatalogService.class);
        ComicImportRepository imports = org.mockito.Mockito.mock(ComicImportRepository.class);
        when(storage.get(owner, attachmentId)).thenReturn(Mono.just(new AttachmentView(attachmentId, UUID.randomUUID(),
            "not-a-comic.pdf", AttachmentKind.ORIGINAL, "b".repeat(64), 10L, "application/pdf", AttachmentAvailabilityStatus.READY)));
        when(imports.findByOwnerIdAndIdempotencyKey(owner, "key")).thenReturn(Mono.empty());

        StepVerifier.create(new PersistentComicImportService(storage, catalog, imports)
                .create(owner, new CreateComicImportRequest(attachmentId, null, "zh-CN"), "key"))
            .expectErrorMessage("仅支持 CBZ、CBR 或 ZIP 漫画包").verify();
        verify(catalog, never()).createWork(any(), any()); verify(imports, never()).save(any());
    }
}
