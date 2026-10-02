package run.ikaros.storage;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.authorization.api.PermissionSnapshotQuery;
import run.ikaros.common.ConflictException;
import run.ikaros.common.ForbiddenException;
import run.ikaros.storage.api.AttachmentAvailabilityStatus;
import run.ikaros.storage.api.AdminAttachmentItem;
import run.ikaros.storage.api.AdminAttachmentPage;

/** Cross-owner attachment listing, guarded by the dedicated platform permission. */
@Service
public final class AdminAttachmentQueryService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final String REQUIRED_PERMISSION = "storage.attachment.manage";

    private final PermissionSnapshotQuery permissionSnapshotQuery;
    private final AttachmentRepository attachments;
    private final BlobRepository blobs;

    public AdminAttachmentQueryService(PermissionSnapshotQuery permissionSnapshotQuery,
                                       AttachmentRepository attachments, BlobRepository blobs) {
        this.permissionSnapshotQuery = permissionSnapshotQuery;
        this.attachments = attachments;
        this.blobs = blobs;
    }

    public Mono<AdminAttachmentPage> listAll(UUID actorId, int page, int size, String query) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            return Mono.error(new IllegalArgumentException("分页参数不合法"));
        }
        return permissionSnapshotQuery.permissionsFor(actorId)
            .filter(snapshot -> snapshot.permissionKeys().contains(REQUIRED_PERMISSION))
            .switchIfEmpty(Mono.error(new ForbiddenException("缺少附件管理权限")))
            .then(Mono.defer(() -> {
                long offset = (long) page * size;
                String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
                var attachmentRows = normalizedQuery == null
                    ? attachments.searchAllActive(offset, size)
                    : attachments.searchAllActiveByQuery(normalizedQuery, offset, size);
                Mono<List<AdminAttachmentItem>> items = attachmentRows
                    .flatMap(attachment -> blobs.findById(attachment.blobId())
                        .switchIfEmpty(Mono.error(new ConflictException("附件引用了不存在的 Blob")))
                        .map(blob -> new AdminAttachmentItem(attachment.id(), attachment.resourceId(),
                            attachment.fileName(), attachment.attachmentKind(), blob.sha256(), blob.sizeBytes(),
                            blob.mediaType(), switch (blob.availability()) {
                                case AVAILABLE -> AttachmentAvailabilityStatus.READY;
                                case PROCESSING -> AttachmentAvailabilityStatus.PROCESSING;
                                case REMOTE, RESTORING -> AttachmentAvailabilityStatus.RESTORE_REQUIRED;
                                case MISSING -> AttachmentAvailabilityStatus.MISSING;
                                case CORRUPTED -> AttachmentAvailabilityStatus.CORRUPTED;
                            }))
                    ).collectList();
                Mono<Long> count = normalizedQuery == null
                    ? attachments.countAllActive() : attachments.countAllActiveByQuery(normalizedQuery);
                return Mono.zip(items, count)
                    .map(result -> new AdminAttachmentPage(result.getT1(), result.getT2(), page, size));
            }));
    }
}
