package run.ikaros.reading;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import run.ikaros.storage.api.AttachmentAvailabilityStatus;
import run.ikaros.storage.api.AttachmentContentService;
import run.ikaros.storage.api.AttachmentKind;
import run.ikaros.storage.api.AttachmentView;
import run.ikaros.storage.api.StorageService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PersistentComicImportParseServiceTest {
    @Test
    void discoversChapterAndNaturalPageOrderFromComicPackage() throws Exception {
        UUID owner = UUID.randomUUID(); UUID importId = UUID.randomUUID(); UUID attachmentId = UUID.randomUUID();
        Instant now = Instant.now();
        ComicImportEntity record = new ComicImportEntity(importId, owner, attachmentId, UUID.randomUUID(),
            UUID.randomUUID(), ComicImportStatus.ACCEPTED.name(), null, null, "key", now, now, 0L);
        StorageService storage = org.mockito.Mockito.mock(StorageService.class);
        AttachmentContentService content = org.mockito.Mockito.mock(AttachmentContentService.class);
        ComicImportRepository imports = org.mockito.Mockito.mock(ComicImportRepository.class);
        ComicImportEntryRepository entries = org.mockito.Mockito.mock(ComicImportEntryRepository.class);
        when(imports.findByIdAndOwnerId(importId, owner)).thenReturn(Mono.just(record));
        when(imports.save(any(ComicImportEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(entries.deleteAllByImportId(importId)).thenReturn(Mono.empty());
        when(entries.save(any(ComicImportEntryEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(storage.get(owner, attachmentId)).thenReturn(Mono.just(new AttachmentView(attachmentId, UUID.randomUUID(),
            "book.cbz", AttachmentKind.ORIGINAL, "a".repeat(64), 100L, "application/zip", AttachmentAvailabilityStatus.READY)));
        when(content.read(owner, attachmentId)).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(zipBytes())));

        StepVerifier.create(new PersistentComicImportParseService(storage, content, imports, entries).parse(owner, importId))
            .expectNextMatches(view -> view.status() == ComicImportStatus.SUCCEEDED).verifyComplete();
        when(entries.findAllByImportIdOrderByChapterKeyAscPageOrderAsc(importId)).thenReturn(Flux.just(
            new ComicImportEntryEntity(UUID.randomUUID(), importId, "chapter-1", "chapter-1/page-1.jpg", 0, "NORMAL", 0L),
            new ComicImportEntryEntity(UUID.randomUUID(), importId, "chapter-1", "chapter-1/page-2.jpg", 1, "NORMAL", 0L)));
        StepVerifier.create(new PersistentComicImportParseService(storage, content, imports, entries).entries(owner, importId))
            .expectNextCount(2).verifyComplete();
    }

    @Test
    void reordersAllPagesInOneChapterAndRejectsPartialLists() {
        UUID owner = UUID.randomUUID(); UUID importId = UUID.randomUUID(); UUID first = UUID.randomUUID(); UUID second = UUID.randomUUID();
        Instant now = Instant.now();
        ComicImportEntity record = new ComicImportEntity(importId, owner, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            ComicImportStatus.SUCCEEDED.name(), null, null, "key", now, now, 0L);
        ComicImportRepository imports = org.mockito.Mockito.mock(ComicImportRepository.class);
        ComicImportEntryRepository entries = org.mockito.Mockito.mock(ComicImportEntryRepository.class);
        when(imports.findByIdAndOwnerId(importId, owner)).thenReturn(Mono.just(record));
        when(entries.findAllByImportIdOrderByChapterKeyAscPageOrderAsc(importId)).thenReturn(Flux.just(
            new ComicImportEntryEntity(first, importId, "chapter-1", "1.jpg", 0, "NORMAL", 0L),
            new ComicImportEntryEntity(second, importId, "chapter-1", "2.jpg", 1, "NORMAL", 0L)));
        when(entries.shiftPageOrders(importId, "chapter-1", 3)).thenReturn(Mono.just(2));
        when(entries.updatePageOrder(importId, second, 0)).thenReturn(Mono.just(1));
        when(entries.updatePageOrder(importId, first, 1)).thenReturn(Mono.just(1));
        when(imports.save(any(ComicImportEntity.class))).thenReturn(Mono.just(record));
        PersistentComicImportParseService service = new PersistentComicImportParseService(
            org.mockito.Mockito.mock(StorageService.class), org.mockito.Mockito.mock(AttachmentContentService.class), imports, entries);

        StepVerifier.create(service.reorder(owner, importId, new ReorderComicPagesRequest("chapter-1", java.util.List.of(second, first))))
            .expectNextMatches(view -> view.status() == ComicImportStatus.SUCCEEDED).verifyComplete();
        StepVerifier.create(service.reorder(owner, importId, new ReorderComicPagesRequest("chapter-1", java.util.List.of(first))))
            .expectErrorMessage("页序调整必须提交该章节全部且不重复的条目").verify();
    }

    @Test
    void recordsExplicitFailureReasonForUnsupportedCbrParser() {
        UUID owner = UUID.randomUUID(); UUID importId = UUID.randomUUID(); UUID attachmentId = UUID.randomUUID();
        Instant now = Instant.now();
        ComicImportEntity record = new ComicImportEntity(importId, owner, attachmentId, UUID.randomUUID(), UUID.randomUUID(),
            ComicImportStatus.ACCEPTED.name(), null, null, "key", now, now, 0L);
        StorageService storage = org.mockito.Mockito.mock(StorageService.class);
        ComicImportRepository imports = org.mockito.Mockito.mock(ComicImportRepository.class);
        ComicImportEntryRepository entries = org.mockito.Mockito.mock(ComicImportEntryRepository.class);
        when(imports.findByIdAndOwnerId(importId, owner)).thenReturn(Mono.just(record));
        when(imports.save(any(ComicImportEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(entries.deleteAllByImportId(importId)).thenReturn(Mono.empty());
        when(storage.get(owner, attachmentId)).thenReturn(Mono.just(new AttachmentView(attachmentId, UUID.randomUUID(),
            "archive.cbr", AttachmentKind.ORIGINAL, "c".repeat(64), 10L, "application/x-rar", AttachmentAvailabilityStatus.READY)));
        PersistentComicImportParseService service = new PersistentComicImportParseService(storage,
            org.mockito.Mockito.mock(AttachmentContentService.class), imports, entries);

        StepVerifier.create(service.parse(owner, importId))
            .expectNextMatches(view -> view.status() == ComicImportStatus.FAILED
                && "CBR 解析器尚未配置，请使用 CBZ 或 ZIP".equals(view.errorMessage()))
            .verifyComplete();
    }

    private byte[] zipBytes() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (String name : new String[] {"chapter-1/page-10.jpg", "chapter-1/page-2.jpg", "chapter-2/cover.png"}) {
                zip.putNextEntry(new ZipEntry(name)); zip.write(1); zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
}
