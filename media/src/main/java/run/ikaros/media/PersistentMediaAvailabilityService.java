package run.ikaros.media;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.resource.api.ResourceService;
import run.ikaros.storage.api.AttachmentAvailabilityQuery;
import run.ikaros.storage.api.AttachmentAvailabilityStatus;

@Service
public class PersistentMediaAvailabilityService implements MediaAvailabilityService {
    private final ResourceService resources;
    private final MediaReleaseRepository releases;
    private final AttachmentAvailabilityQuery attachmentAvailability;

    public PersistentMediaAvailabilityService(ResourceService resources, MediaReleaseRepository releases,
        AttachmentAvailabilityQuery attachmentAvailability) {
        this.resources = resources; this.releases = releases; this.attachmentAvailability = attachmentAvailability;
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
        return attachmentAvailability.get(release.ownerId(), release.attachmentId())
            .map(availability -> fromAttachment(release, availability.status()))
            .defaultIfEmpty(new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.MISSING, release.id(), "Attachment 或 Blob 不存在"));
    }

    private MediaAvailabilityView fromAttachment(MediaReleaseEntity release, AttachmentAvailabilityStatus status) {
        return switch (status) {
            case READY -> new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.AVAILABLE, release.id(), "存在可读 Storage Placement");
            case PROCESSING -> new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.PROCESSING, release.id(), "Blob 仍在处理");
            case RESTORE_REQUIRED -> new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.RESTORE_REQUIRED, release.id(), "Blob 需要恢复");
            case MISSING -> new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.MISSING, release.id(), "Blob 不存在或没有可读 Storage Placement");
            case CORRUPTED -> new MediaAvailabilityView(release.playableResourceId(), MediaAvailability.CORRUPTED, release.id(), "Blob 校验失败");
        };
    }
}
