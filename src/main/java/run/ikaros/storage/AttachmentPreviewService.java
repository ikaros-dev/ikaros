package run.ikaros.storage;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.StorageUnavailableException;
import run.ikaros.resource.ResourceRepository;

@Service
public class AttachmentPreviewService {
    private final AttachmentRepository attachments;
    private final ResourceRepository resources;
    private final BlobRepository blobs;
    private final BlobPlacementRepository placements;
    private final StorageProviderRegistry providers;
    private final StorageObjectProviderRegistry objects;

    public AttachmentPreviewService(AttachmentRepository attachments, ResourceRepository resources, BlobRepository blobs,
                                     BlobPlacementRepository placements, StorageProviderRegistry providers,
                                     StorageObjectProviderRegistry objects) {
        this.attachments = attachments; this.resources = resources; this.blobs = blobs; this.placements = placements;
        this.providers = providers; this.objects = objects;
    }

    public Mono<AttachmentPreviewUrlView> issue(UUID actorId, UUID attachmentId) {
        return attachments.findById(attachmentId).filter(a -> a.deletedAt() == null)
            .flatMap(attachment -> resources.findByIdAndOwnerId(attachment.resourceId(), actorId).thenReturn(attachment))
            .switchIfEmpty(Mono.error(new NotFoundException("Attachment 不存在或无权访问")))
            .flatMap(attachment -> blobs.findById(attachment.blobId())
                .switchIfEmpty(Mono.error(new NotFoundException("Attachment 对应的 Blob 不存在")))
                .flatMap(blob -> placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                    .filter(placement -> placement.placementState() == PlacementState.ACTIVE)
                    .concatMap(placement -> providers.getByKey(placement.provider())
                        .flatMap(provider -> objects.createReadIntent(provider, placement.objectKey())
                            .map(intent -> new AttachmentPreviewUrlView(intent.method(), intent.url(), intent.expiresAt(), true,
                                blob.mediaType()))))
                    .next()
                    .switchIfEmpty(Mono.error(new StorageUnavailableException("附件没有可用存储 Placement")))));
    }
}
