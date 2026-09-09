package run.ikaros.reading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.storage.api.AttachmentContentService;

@Service
public class PersistentEbookTocParseService implements EbookTocParseService {
    private final AttachmentContentService content; private final EbookImportRepository imports; private final EbookChapterRepository chapters; private final ReadingCatalogService catalog;
    public PersistentEbookTocParseService(AttachmentContentService content, EbookImportRepository imports, EbookChapterRepository chapters, ReadingCatalogService catalog) { this.content=content; this.imports=imports; this.chapters=chapters; this.catalog=catalog; }
    @Override public Mono<EbookImportView> parse(UUID ownerId, UUID importId) {
        return imports.findByIdAndOwnerId(importId, ownerId).switchIfEmpty(Mono.error(new NotFoundException("电子书导入不存在或无权访问"))).flatMap(record -> {
            if (ComicImportStatus.SUCCEEDED.name().equals(record.status())) return Mono.just(record);
            if (ComicImportStatus.PARSING.name().equals(record.status())) return Mono.error(new ConflictException("电子书目录正在解析"));
            return imports.save(copy(record, ComicImportStatus.PARSING.name(), null, null)).thenMany(scan(ownerId, record)).single()
                .flatMap(items -> chapters.deleteAllByImportId(importId).thenMany(Flux.fromIterable(items).index().concatMap(item -> catalog.createChapter(ownerId, record.editionId(), null, new CreateReadingChapterRequest(String.valueOf(item.getT1()+1), item.getT2().title(), item.getT2().title(), item.getT1().intValue(), "XHTML", null, null)).flatMap(chapter -> chapters.save(new EbookChapterEntity(null, importId, chapter.id(), item.getT2().href(), item.getT2().title(), item.getT1().intValue()))))).then(imports.save(copy(record, ComicImportStatus.SUCCEEDED.name(), null, null))))
                .onErrorResume(error -> chapters.deleteAllByImportId(importId).then(imports.save(copy(record, ComicImportStatus.FAILED.name(), "ebook.toc_parse_failed", safe(error)))));
        }).map(this::view);
    }
    @Override public Flux<EbookChapterView> chapters(UUID ownerId, UUID importId) { return imports.findByIdAndOwnerId(importId, ownerId).switchIfEmpty(Mono.error(new NotFoundException("电子书导入不存在或无权访问"))).flatMapMany(v -> chapters.findAllByImportIdOrderBySortOrderAsc(importId).map(this::view)); }
    private Mono<List<TocItem>> scan(UUID ownerId, EbookImportEntity record) { return Mono.fromCallable(() -> Files.createTempFile("ikaros-ebook-", ".epub")).subscribeOn(Schedulers.boundedElastic()).flatMap(path -> Mono.usingWhen(Mono.just(path), ignored -> DataBufferUtils.write(content.read(ownerId, record.sourceAttachmentId()), path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).then(Mono.fromCallable(() -> readToc(path)).subscribeOn(Schedulers.boundedElastic())), path2 -> Mono.fromRunnable(() -> { try { Files.deleteIfExists(path2); } catch (IOException ignored) {} }).subscribeOn(Schedulers.boundedElastic()).then())); }
    private List<TocItem> readToc(Path path) throws Exception { try (ZipFile zip=new ZipFile(path.toFile())) { String opf=findRootfile(zip); Document doc=parse(zip.getInputStream(zip.getEntry(opf))); Map<String,String> hrefs=new LinkedHashMap<>(); Map<String,String> titles=new LinkedHashMap<>(); NodeList manifest=doc.getElementsByTagNameNS("*","item"); for(int i=0;i<manifest.getLength();i++){Element e=(Element)manifest.item(i); hrefs.put(e.getAttribute("id"),resolve(opf,e.getAttribute("href"))); titles.put(e.getAttribute("id"),e.getAttribute("properties"));} List<TocItem> result=new ArrayList<>(); NodeList spine=doc.getElementsByTagNameNS("*","itemref"); for(int i=0;i<spine.getLength();i++){Element e=(Element)spine.item(i); String id=e.getAttribute("idref"), href=hrefs.get(id); if(href!=null) result.add(new TocItem(href,title(zip,href,i),i));} if(result.isEmpty()) throw new ConflictException("EPUB 未找到可阅读章节"); return result; } }
    private String findRootfile(ZipFile zip) throws Exception { Document d=parse(zip.getInputStream(zip.getEntry("META-INF/container.xml"))); NodeList nodes=d.getElementsByTagNameNS("*","rootfile"); if(nodes.getLength()==0) throw new ConflictException("EPUB 缺少 rootfile"); return ((Element)nodes.item(0)).getAttribute("full-path"); }
    private Document parse(java.io.InputStream in) throws Exception { DocumentBuilderFactory f=DocumentBuilderFactory.newInstance(); f.setNamespaceAware(true); f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true); f.setFeature("http://xml.org/sax/features/external-general-entities",false); f.setFeature("http://xml.org/sax/features/external-parameter-entities",false); return f.newDocumentBuilder().parse(in); }
    private String resolve(String opf,String href) { String base=opf.replace('\\','/'); int slash=base.lastIndexOf('/'); String dir=slash<0?"":base.substring(0,slash+1); return java.net.URI.create(dir).resolve(href).normalize().toString(); }
    private String title(ZipFile zip,String href,int index) { return href.substring(href.lastIndexOf('/')+1).replaceFirst("\\.[^.]+$", ""); }
    private Mono<Void> noop(){return Mono.empty();}
    private EbookImportEntity copy(EbookImportEntity e,String status,String code,String msg){return new EbookImportEntity(e.id(),e.ownerId(),e.sourceAttachmentId(),e.workId(),e.editionId(),status,code,msg,e.idempotencyKey(),e.createdAt(),java.time.Instant.now(),e.version());}
    private EbookImportView view(EbookImportEntity e){return new EbookImportView(e.id(),e.sourceAttachmentId(),e.workId(),e.editionId(),ComicImportStatus.valueOf(e.status()),e.errorCode(),e.errorMessage(),e.createdAt(),e.updatedAt());}
    private EbookChapterView view(EbookChapterEntity e){return new EbookChapterView(e.id(),e.importId(),e.chapterId(),e.href(),e.title(),e.sortOrder());}
    private String safe(Throwable e){String m=e.getMessage();return m==null||m.isBlank()?"电子书目录解析失败":m.substring(0,Math.min(512,m.length()));}
    private record TocItem(String href,String title,int index){}
}
