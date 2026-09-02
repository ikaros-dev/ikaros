package run.ikaros.media;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.resource.ResourceService;
import run.ikaros.storage.AttachmentRepository;
import run.ikaros.storage.BlobAvailability;
import run.ikaros.storage.BlobEntity;
import run.ikaros.storage.BlobRepository;
import run.ikaros.storage.BlobPlacementRepository;
import run.ikaros.storage.PlacementState;
import run.ikaros.storage.StorageProviderRegistry;
import run.ikaros.storage.StorageProviderStatus;

@Service
public class PersistentMediaAvailabilityService implements MediaAvailabilityService {
    private final ResourceService resources;
    private final MediaReleaseRepository releases;
    private final AttachmentRepository attachments;
    private final BlobRepository blobs;
    private final BlobPlacementRepository placements;
    private final StorageProviderRegistry providerRegistry;

    public PersistentMediaAvailabilityService(ResourceService resources, MediaReleaseRepository releases,
        AttachmentRepository attachments, BlobRepository blobs, BlobPlacementRepository placements,
        StorageProviderRegistry providerRegistry) {
        this.resources = resources; this.releases = releases; this.attachments = attachments;
        this.blobs = blobs; this.placements = placements; this.providerRegistry = providerRegistry;
    }

    @Override
    public Mono<MediaAvailabilityView> get(UUID ownerId, UUID resourceId) {
        return resources.get(ownerId, resourceId)
            .then(releases.findAllByOwnerIdAndPlayableResourceIdOrderByCreatedAtDesc(ownerId, resourceId)
                .filter(release -> release.state() != MediaReleaseState.ARCHIVED)
                .next()
                .flatMap(this::availability)
            .switchIfEmpty(releases.findAllByOwnerIdAndPlayableResourceIdOrderByCreatedAtDesc(ownerId, resourceId)
                .next()
                .map(release -> new MediaAvailabilityView(resourceId, MediaAvailability.RESTORE_REQUIRED, release.id(), "Release 已归档，需要恢复")))
            .defaultIfEmpty(new MediaAvailabilityView(resourceId, MediaAvailability.MISSING, null, "没有关联的 Media Release")));
    }

    private Mono<MediaAvailabilityView> availability(MediaReleaseEntity release) {
        if (release.state() == MediaReleaseState.CORRUPTED) {
            return Mono.just(new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.CORRUPTED, release.id(), "最近 Release 校验失败"));
        }
        if (release.state() == MediaReleaseState.MISSING) {
            return Mono.just(new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.MISSING, release.id(), "最近 Release 的内容不可用"));
        }
        return attachments.findById(release.attachmentId())
            .flatMap(attachment -> blobs.findById(attachment.blobId()))
            .flatMap(blob -> fromBlob(release, blob))
            .defaultIfEmpty(new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.MISSING, release.id(), "Attachment 或 Blob 不存在"));
    }

    private Mono<MediaAvailabilityView> fromBlob(MediaReleaseEntity release, BlobEntity blob) {
        return switch (blob.availability()) {
            case RESTORING -> Mono.just(new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.RESTORE_REQUIRED, release.id(), "Blob 正在恢复"));
            case PROCESSING -> Mono.just(new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.PROCESSING, release.id(), "Blob 仍在处理"));
            case MISSING -> Mono.just(new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.MISSING, release.id(), "Blob 不存在"));
            case CORRUPTED -> Mono.just(new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.CORRUPTED, release.id(), "Blob 校验失败"));
            case AVAILABLE, REMOTE -> placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                .filter(placement -> placement.placementState() == PlacementState.ACTIVE)
                .concatMap(placement -> providerRegistry.getByKey(placement.provider())
                    .map(provider -> provider.status() != StorageProviderStatus.DISABLED
                        && provider.status() != StorageProviderStatus.FAILED))
                .any(Boolean::booleanValue)
                .map(readable -> readable
                    ? new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.AVAILABLE, release.id(), "存在可读 Storage Placement")
                    : new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.MISSING, release.id(), "Blob 没有可读 Storage Placement"));
        };
    }
}
