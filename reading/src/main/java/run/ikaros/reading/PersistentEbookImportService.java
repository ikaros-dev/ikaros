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
public class PersistentEbookImportService implements EbookImportService {
    private final StorageService storage; private final ReadingCatalogService catalog; private final EbookImportRepository imports;
    public PersistentEbookImportService(StorageService storage, ReadingCatalogService catalog, EbookImportRepository imports) { this.storage=storage; this.catalog=catalog; this.imports=imports; }
    @Override public Mono<EbookImportView> create(UUID ownerId, CreateEbookImportRequest request, String key) {
        if (key == null || key.isBlank() || key.length() > 256) return Mono.error(new IllegalArgumentException("Idempotency-Key 不合法"));
        Mono<EbookImportView> create = storage.get(ownerId, request.attachmentId()).flatMap(this::validate)
            .flatMap(attachment -> { String title = request.title() == null || request.title().isBlank() ? baseName(attachment.fileName()) : request.title().trim();
                return catalog.createWork(ownerId, new CreateReadingWorkRequest(title, ReadingWorkKind.EBOOK, request.language(), request.language()))
                    .flatMap(work -> catalog.createEdition(ownerId, work.id(), new CreateReadingEditionRequest("电子书导入", request.language(), null, attachment.fileName(), 0))
                        .map(edition -> new EbookImportEntity(null, ownerId, attachment.id(), work.id(), edition.id(), ComicImportStatus.ACCEPTED.name(), null, null, key, Instant.now(), Instant.now(), null))); })
            .flatMap(imports::save).map(this::view);
        return imports.findByOwnerIdAndIdempotencyKey(ownerId, key).map(this::view).switchIfEmpty(create)
            .onErrorResume(DuplicateKeyException.class, error -> imports.findByOwnerIdAndIdempotencyKey(ownerId, key).switchIfEmpty(Mono.error(error)).map(this::view));
    }
    @Override public Flux<EbookImportView> list(UUID ownerId) { return imports.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).take(100).map(this::view); }
    @Override public Mono<EbookImportView> get(UUID ownerId, UUID importId) { return imports.findByIdAndOwnerId(importId, ownerId).switchIfEmpty(Mono.error(new NotFoundException("电子书导入不存在或无权访问"))).map(this::view); }
    private Mono<AttachmentView> validate(AttachmentView attachment) { String name=attachment.fileName()==null?"":attachment.fileName().toLowerCase(Locale.ROOT); if (!name.endsWith(".epub")) return Mono.error(new ConflictException("仅支持 EPUB 电子书")); return Mono.just(attachment); }
    private String baseName(String value) { String name=value==null?"未命名电子书":value; int slash=Math.max(name.lastIndexOf('/'),name.lastIndexOf('\\')); if(slash>=0)name=name.substring(slash+1); int dot=name.lastIndexOf('.'); return (dot>0?name.substring(0,dot):name).trim(); }
    private EbookImportView view(EbookImportEntity e) { return new EbookImportView(e.id(),e.sourceAttachmentId(),e.workId(),e.editionId(),ComicImportStatus.valueOf(e.status()),e.errorCode(),e.errorMessage(),e.createdAt(),e.updatedAt()); }
}
