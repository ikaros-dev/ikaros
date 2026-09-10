package run.ikaros.reading;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.storage.api.AttachmentAvailabilityStatus;
import run.ikaros.storage.api.AttachmentKind;
import run.ikaros.storage.api.AttachmentView;
import run.ikaros.storage.api.StorageService;

class PersistentEbookImportServiceTest {
    @Test void acceptsEpubAndCreatesEbookWork() {
        UUID owner=UUID.randomUUID(), attachment=UUID.randomUUID(), work=UUID.randomUUID(), edition=UUID.randomUUID(), id=UUID.randomUUID();
        StorageService storage=org.mockito.Mockito.mock(StorageService.class); ReadingCatalogService catalog=org.mockito.Mockito.mock(ReadingCatalogService.class); EbookImportRepository repo=org.mockito.Mockito.mock(EbookImportRepository.class);
        when(repo.findByOwnerIdAndIdempotencyKey(owner,"key")).thenReturn(Mono.empty());
        when(storage.get(owner,attachment)).thenReturn(Mono.just(new AttachmentView(attachment,UUID.randomUUID(),"book.epub",AttachmentKind.ORIGINAL,"a".repeat(64),10,"application/epub+zip",AttachmentAvailabilityStatus.READY)));
        when(catalog.createWork(any(),any())).thenReturn(Mono.just(new ReadingWorkView(work,UUID.randomUUID(),ReadingWorkKind.EBOOK,"zh-CN")));
        when(catalog.createEdition(any(),any(),any())).thenReturn(Mono.just(new ReadingEditionView(edition,work,"电子书导入","zh-CN",null,"book.epub",0,"AVAILABLE")));
        Instant now=Instant.now(); when(repo.save(any())).thenReturn(Mono.just(new EbookImportEntity(id,owner,attachment,work,edition,ComicImportStatus.ACCEPTED.name(),null,null,"key",now,now,0L)));
        StepVerifier.create(new PersistentEbookImportService(storage,catalog,repo).create(owner,new CreateEbookImportRequest(attachment,null,"zh-CN"),"key")).expectNextMatches(v->v.id().equals(id)&&v.workId().equals(work)).verifyComplete();
        verify(catalog).createWork(any(),any()); verify(repo).save(any(EbookImportEntity.class));
    }
    @Test void rejectsNonEpubBeforeCreatingRecords() {
        UUID owner=UUID.randomUUID(), attachment=UUID.randomUUID(); StorageService storage=org.mockito.Mockito.mock(StorageService.class); ReadingCatalogService catalog=org.mockito.Mockito.mock(ReadingCatalogService.class); EbookImportRepository repo=org.mockito.Mockito.mock(EbookImportRepository.class);
        when(repo.findByOwnerIdAndIdempotencyKey(owner,"key")).thenReturn(Mono.empty()); when(storage.get(owner,attachment)).thenReturn(Mono.just(new AttachmentView(attachment,UUID.randomUUID(),"book.pdf",AttachmentKind.ORIGINAL,"b".repeat(64),10,"application/pdf",AttachmentAvailabilityStatus.READY)));
        StepVerifier.create(new PersistentEbookImportService(storage,catalog,repo).create(owner,new CreateEbookImportRequest(attachment,null,"zh-CN"),"key")).expectErrorMessage("仅支持 EPUB 电子书").verify(); verify(catalog,never()).createWork(any(),any()); verify(repo,never()).save(any());
    }
}
