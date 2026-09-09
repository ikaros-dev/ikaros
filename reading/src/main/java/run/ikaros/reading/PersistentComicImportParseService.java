package run.ikaros.reading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.storage.api.AttachmentContentService;
import run.ikaros.storage.api.StorageService;

@Service
public class PersistentComicImportParseService implements ComicImportParseService {
    private final StorageService storage;
    private final AttachmentContentService content;
    private final ComicImportRepository imports;
    private final ComicImportEntryRepository entries;

    public PersistentComicImportParseService(StorageService storage, AttachmentContentService content,
        ComicImportRepository imports, ComicImportEntryRepository entries) {
        this.storage = storage; this.content = content; this.imports = imports; this.entries = entries;
    }

    @Override
    public Mono<ComicImportView> parse(UUID ownerId, UUID importId) {
        return imports.findByIdAndOwnerId(importId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("漫画导入不存在或无权访问")))
            .flatMap(importRecord -> {
                if (ComicImportStatus.SUCCEEDED.name().equals(importRecord.status())) return Mono.just(importRecord);
                if (ComicImportStatus.PARSING.name().equals(importRecord.status()))
                    return Mono.error(new ConflictException("漫画包正在解析"));
                ComicImportEntity parsing = copy(importRecord, ComicImportStatus.PARSING.name(), null, null);
                return imports.save(parsing)
                    .thenMany(storage.get(ownerId, importRecord.sourceAttachmentId()).flux())
                    .flatMap(attachment -> {
                        String name = attachment.fileName() == null ? "" : attachment.fileName().toLowerCase(Locale.ROOT);
                        if (name.endsWith(".cbr")) return Mono.error(new ConflictException("CBR 解析器尚未配置，请使用 CBZ 或 ZIP"));
                        if (!(name.endsWith(".cbz") || name.endsWith(".zip")))
                            return Mono.error(new ConflictException("源附件不是支持的漫画包"));
                        return scanPackage(ownerId, importRecord, name);
                    }).single()
                    .flatMap(found -> entries.deleteAllByImportId(importId).thenMany(Flux.fromIterable(found)
                        .map(entry -> new ComicImportEntryEntity(null, importId, entry.chapterKey(), entry.entryName(),
                            entry.pageOrder(), entry.pageRole(), null)))
                        .flatMapSequential(entries::save).then(imports.save(copy(importRecord,
                            ComicImportStatus.SUCCEEDED.name(), null, null))))
                    .onErrorResume(error -> entries.deleteAllByImportId(importId)
                        .then(imports.save(copy(importRecord, ComicImportStatus.FAILED.name(), "comic.parse_failed", safeMessage(error)))));
            }).map(this::view);
    }

    @Override public Flux<ComicImportEntryEntity> entries(UUID ownerId, UUID importId) {
        return imports.findByIdAndOwnerId(importId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("漫画导入不存在或无权访问")))
            .flatMapMany(value -> entries.findAllByImportIdOrderByChapterKeyAscPageOrderAsc(importId));
    }

    @Override public Mono<ComicImportView> reorder(UUID ownerId, UUID importId, ReorderComicPagesRequest request) {
        return imports.findByIdAndOwnerId(importId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("漫画导入不存在或无权访问")))
            .flatMap(value -> entries.findAllByImportIdOrderByChapterKeyAscPageOrderAsc(importId)
                .filter(entry -> request.chapterKey().equals(entry.chapterKey())).collectList()
                .flatMap(current -> {
                    java.util.Set<UUID> expected = current.stream().map(ComicImportEntryEntity::id).collect(java.util.stream.Collectors.toSet());
                    java.util.Set<UUID> requested = new java.util.HashSet<>(request.entryIds());
                    if (expected.isEmpty() || expected.size() != requested.size() || !expected.equals(requested))
                        return Mono.error(new ConflictException("页序调整必须提交该章节全部且不重复的条目"));
                    return entries.shiftPageOrders(importId, request.chapterKey(), current.size() + 1)
                        .thenMany(Flux.fromIterable(request.entryIds()).index()
                            .concatMap(item -> entries.updatePageOrder(importId, item.getT2(), item.getT1().intValue())))
                        .then(imports.save(copy(value, value.status(), value.errorCode(), value.errorMessage())));
                })).map(this::view);
    }

    private Mono<List<ComicImportEntryEntity>> scanPackage(UUID ownerId, ComicImportEntity record, String extension) {
        return Mono.fromCallable(() -> Files.createTempFile("ikaros-comic-", extension))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(path -> Mono.usingWhen(
                Mono.just(path),
                ignored -> DataBufferUtils.write(content.read(ownerId, record.sourceAttachmentId()), path,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                    .then(Mono.fromCallable(() -> scan(path)).subscribeOn(Schedulers.boundedElastic())),
                ignored -> delete(path)))
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> delete(Path path) {
        return Mono.fromRunnable(() -> { try { Files.deleteIfExists(path); } catch (IOException ignored) { } })
            .subscribeOn(Schedulers.boundedElastic()).then();
    }

    private List<ComicImportEntryEntity> scan(Path path) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            zip.stream().filter(entry -> !entry.isDirectory() && image(entry.getName()))
                .map(ZipEntry::getName).forEach(names::add);
        }
        names.sort(Comparator.comparing(this::chapter).thenComparing(this::naturalName));
        List<ComicImportEntryEntity> result = new ArrayList<>(); String chapter = null; int order = 0;
        for (String name : names) {
            String current = chapter(name); if (!current.equals(chapter)) { chapter = current; order = 0; }
            result.add(new ComicImportEntryEntity(null, null, current, name, order++, role(name), null));
        }
        return result;
    }

    private boolean image(String name) { String n = name.toLowerCase(Locale.ROOT); return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp") || n.endsWith(".gif") || n.endsWith(".avif"); }
    private String chapter(String name) { int slash = name.indexOf('/'); return slash > 0 ? name.substring(0, slash) : "chapter-1"; }
    private String naturalName(String name) { return name.toLowerCase(Locale.ROOT).replaceAll("(\\d+)", "0000000000$1"); }
    private String role(String name) { return name.toLowerCase(Locale.ROOT).contains("cover") ? "COVER" : "NORMAL"; }
    private ComicImportEntity copy(ComicImportEntity value, String status, String code, String message) { return new ComicImportEntity(value.id(), value.ownerId(), value.sourceAttachmentId(), value.workId(), value.editionId(), status, code, message, value.idempotencyKey(), value.createdAt(), java.time.Instant.now(), value.version()); }
    private ComicImportView view(ComicImportEntity value) { return new ComicImportView(value.id(), value.sourceAttachmentId(), value.workId(), value.editionId(), ComicImportStatus.valueOf(value.status()), value.errorCode(), value.errorMessage(), value.createdAt(), value.updatedAt()); }
    private String safeMessage(Throwable error) { String message = error.getMessage(); return message == null || message.isBlank() ? "漫画包解析失败" : message.substring(0, Math.min(512, message.length())); }
}
