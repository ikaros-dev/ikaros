package run.ikaros.reading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import run.ikaros.common.NotFoundException;
import run.ikaros.storage.api.AttachmentContentService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class PersistentEbookChapterContentService implements EbookChapterContentService {
    private final AttachmentContentService content;
    private final EbookImportRepository imports;
    private final EbookChapterRepository chapters;

    public PersistentEbookChapterContentService(AttachmentContentService content,
                                                EbookImportRepository imports,
                                                EbookChapterRepository chapters) {
        this.content = content;
        this.imports = imports;
        this.chapters = chapters;
    }

    @Override
    public Mono<EbookChapterContentView> get(UUID ownerId, UUID importId, UUID chapterId) {
        return imports.findByIdAndOwnerId(importId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("电子书导入不存在或无权访问")))
            .flatMap(record -> chapters.findById(chapterId)
                .filter(chapter -> chapter.importId().equals(importId))
                .switchIfEmpty(Mono.error(new NotFoundException("电子书章节不存在或无权访问")))
                .flatMap(chapter -> readChapter(ownerId, record.sourceAttachmentId(), chapter)));
    }

    private Mono<EbookChapterContentView> readChapter(UUID ownerId, UUID attachmentId, EbookChapterEntity chapter) {
        return Mono.fromCallable(() -> Files.createTempFile("ikaros-ebook-content-", ".epub"))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(path -> Mono.usingWhen(Mono.just(path), ignored ->
                DataBufferUtils.write(content.read(ownerId, attachmentId), path,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                    .then(Mono.fromCallable(() -> extractText(path, chapter.href()))
                        .subscribeOn(Schedulers.boundedElastic())),
                this::deleteTempFile))
            .map(text -> new EbookChapterContentView(chapter.chapterId(), chapter.title(), chapter.href(), text));
    }

    private String extractText(Path path, String href) throws Exception {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            var entry = zip.getEntry(href);
            if (entry == null) {
                throw new NotFoundException("电子书章节内容不存在");
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder().parse(zip.getInputStream(entry));
            String text = document.getDocumentElement().getTextContent().replaceAll("\\s+", " ").trim();
            if (text.isBlank()) {
                throw new NotFoundException("电子书章节没有可阅读正文");
            }
            return text;
        }
    }

    private Mono<Void> deleteTempFile(Path path) {
        return Mono.fromRunnable(() -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // Temporary cleanup is best effort; the content result is already complete.
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
