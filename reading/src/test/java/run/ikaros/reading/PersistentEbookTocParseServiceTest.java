package run.ikaros.reading;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.storage.api.AttachmentContentService;

class PersistentEbookTocParseServiceTest {
    @Test void parsesEpubSpineInStableOrder() throws Exception {
        UUID owner=UUID.randomUUID(), importId=UUID.randomUUID(), attachment=UUID.randomUUID(), edition=UUID.randomUUID(); Instant now=Instant.now();
        EbookImportEntity record=new EbookImportEntity(importId,owner,attachment,UUID.randomUUID(),edition,ComicImportStatus.ACCEPTED.name(),null,null,"key",now,now,0L);
        AttachmentContentService content=org.mockito.Mockito.mock(AttachmentContentService.class); EbookImportRepository imports=org.mockito.Mockito.mock(EbookImportRepository.class); EbookChapterRepository chapters=org.mockito.Mockito.mock(EbookChapterRepository.class); ReadingCatalogService catalog=org.mockito.Mockito.mock(ReadingCatalogService.class);
        when(imports.findByIdAndOwnerId(importId,owner)).thenReturn(Mono.just(record)); when(imports.save(any())).thenAnswer(i->Mono.just(i.getArgument(0))); when(chapters.deleteAllByImportId(importId)).thenReturn(Mono.empty()); when(chapters.save(any())).thenAnswer(i->Mono.just(i.getArgument(0))); when(content.read(owner,attachment)).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(epubBytes())));
        when(catalog.createChapter(any(),any(),any(),any())).thenAnswer(i -> { CreateReadingChapterRequest request=i.getArgument(3); return Mono.just(new ReadingChapterView(UUID.randomUUID(),record.workId(),edition,null,UUID.randomUUID(),request.structuredNumber(),request.displayLabel(),request.title(),request.sortOrder(),request.contentKind(),null,null,"AVAILABLE")); });
        StepVerifier.create(new PersistentEbookTocParseService(content,imports,chapters,catalog).parse(owner,importId)).expectNextMatches(v->v.status()==ComicImportStatus.SUCCEEDED).verifyComplete();
    }
    @Test void marksCorruptContainerFailedAndClearsEntries() {
        UUID owner=UUID.randomUUID(), importId=UUID.randomUUID(), attachment=UUID.randomUUID(), edition=UUID.randomUUID(); Instant now=Instant.now();
        EbookImportEntity record=new EbookImportEntity(importId,owner,attachment,UUID.randomUUID(),edition,ComicImportStatus.ACCEPTED.name(),null,null,"key",now,now,0L);
        AttachmentContentService content=org.mockito.Mockito.mock(AttachmentContentService.class); EbookImportRepository imports=org.mockito.Mockito.mock(EbookImportRepository.class); EbookChapterRepository chapters=org.mockito.Mockito.mock(EbookChapterRepository.class); ReadingCatalogService catalog=org.mockito.Mockito.mock(ReadingCatalogService.class);
        when(imports.findByIdAndOwnerId(importId,owner)).thenReturn(Mono.just(record)); when(imports.save(any())).thenAnswer(i->Mono.just(i.getArgument(0))); when(chapters.deleteAllByImportId(importId)).thenReturn(Mono.empty()); when(content.read(owner,attachment)).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap("not-an-epub".getBytes(StandardCharsets.UTF_8))));
        StepVerifier.create(new PersistentEbookTocParseService(content,imports,chapters,catalog).parse(owner,importId)).expectNextMatches(v->v.status()==ComicImportStatus.FAILED && "ebook.toc_parse_failed".equals(v.errorCode()) && v.errorMessage()!=null).verifyComplete();
        org.mockito.Mockito.verify(chapters).deleteAllByImportId(importId); org.mockito.Mockito.verify(catalog, org.mockito.Mockito.never()).createChapter(any(),any(),any(),any());
    }
    private byte[] epubBytes() throws Exception { ByteArrayOutputStream output=new ByteArrayOutputStream(); try(ZipOutputStream zip=new ZipOutputStream(output)){ add(zip,"META-INF/container.xml","<container><rootfiles><rootfile full-path=\"OEBPS/content.opf\"/></rootfiles></container>"); add(zip,"OEBPS/content.opf","<package xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\"><manifest><item id=\"c1\" href=\"chapter-1.xhtml\"/><item id=\"c2\" href=\"chapter-2.xhtml\"/></manifest><spine><itemref idref=\"c2\"/><itemref idref=\"c1\"/></spine></package>"); } return output.toByteArray(); }
    private void add(ZipOutputStream zip,String name,String value) throws Exception { zip.putNextEntry(new ZipEntry(name)); zip.write(value.getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
}
