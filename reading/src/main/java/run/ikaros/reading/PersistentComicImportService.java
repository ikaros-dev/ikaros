package run.ikaros.reading;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.storage.api.AttachmentView;
import run.ikaros.storage.api.StorageService;

@Service
public class PersistentComicImportService implements ComicImportService {
    private final StorageService storage;
    private final ReadingCatalogService catalog;
    private final ComicImportRepository imports;

    public PersistentComicImportService(StorageService storage, ReadingCatalogService catalog,
        ComicImportRepository imports) {
        this.storage = storage; this.catalog = catalog; this.imports = imports;
    }

    @Override
    public Mono<ComicImportView> create(UUID ownerId, CreateComicImportRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 256)
            return Mono.error(new IllegalArgumentException("Idempotency-Key 不合法"));
        Mono<ComicImportView> create = storage.get(ownerId, request.attachmentId()).flatMap(this::validatePackage)
            .flatMap(attachment -> {
                String title = request.title() == null || request.title().isBlank()
                    ? baseName(attachment.fileName()) : request.title().trim();
                return catalog.createWork(ownerId, new CreateReadingWorkRequest(title, ReadingWorkKind.COMIC,
                        request.language(), request.language()))
                    .flatMap(work -> catalog.createEdition(ownerId, work.id(),
                        new CreateReadingEditionRequest("导入包", request.language(), null,
                            attachment.fileName(), 0)).map(edition -> new ComicImportEntity(null, ownerId,
                            attachment.id(), work.id(), edition.id(), ComicImportStatus.ACCEPTED.name(), null,
                            null, idempotencyKey, Instant.now(), Instant.now(), null)));
            }).flatMap(imports::save).map(this::view);
        return imports.findByOwnerIdAndIdempotencyKey(ownerId, idempotencyKey).map(this::view)
            .switchIfEmpty(create)
            .onErrorResume(DuplicateKeyException.class, e -> imports.findByOwnerIdAndIdempotencyKey(ownerId, idempotencyKey)
                .switchIfEmpty(Mono.error(e)).map(this::view));
    }

    @Override public Flux<ComicImportView> list(UUID ownerId) {
        return imports.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).take(100).map(this::view);
    }

    @Override public Mono<ComicImportView> get(UUID ownerId, UUID importId) {
        return imports.findByIdAndOwnerId(importId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("漫画导入不存在或无权访问"))).map(this::view);
    }

    private Mono<AttachmentView> validatePackage(AttachmentView attachment) {
        String name = attachment.fileName() == null ? "" : attachment.fileName().toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".cbz") || name.endsWith(".cbr") || name.endsWith(".zip")))
            return Mono.error(new ConflictException("仅支持 CBZ、CBR 或 ZIP 漫画包"));
        return Mono.just(attachment);
    }

    private String baseName(String fileName) {
        String value = fileName == null ? "未命名漫画" : fileName;
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        if (slash >= 0) value = value.substring(slash + 1);
        int dot = value.lastIndexOf('.');
        return (dot > 0 ? value.substring(0, dot) : value).trim();
    }

    private ComicImportView view(ComicImportEntity e) {
        return new ComicImportView(e.id(), e.sourceAttachmentId(), e.workId(), e.editionId(),
            ComicImportStatus.valueOf(e.status()), e.errorCode(), e.errorMessage(), e.createdAt(), e.updatedAt());
    }
}
