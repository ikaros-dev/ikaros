package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.ResourceService;
import run.ikaros.resource.ResourceType;
import run.ikaros.storage.AttachmentRepository;

@Service
public class PersistentMediaReleaseService implements MediaReleaseService {
    private final ResourceService resources;
    private final AttachmentRepository attachments;
    private final MediaReleaseRepository releases;

    public PersistentMediaReleaseService(ResourceService resources, AttachmentRepository attachments,
        MediaReleaseRepository releases) { this.resources = resources; this.attachments = attachments; this.releases = releases; }

    @Override public Mono<MediaReleaseView> add(UUID ownerId, UUID resourceId, CreateMediaReleaseRequest request) {
        return resources.get(ownerId, resourceId).flatMap(resource -> {
            if (resource.type() != ResourceType.VIDEO) return Mono.error(new ConflictException("只有 VIDEO Resource 可以添加 Media Release"));
            return attachments.findByIdAndResourceIdAndArchivedAtIsNullAndDeletedAtIsNull(request.attachmentId(), resourceId)
                .switchIfEmpty(Mono.error(new NotFoundException("Attachment 不属于该 Resource")))
                .flatMap(attachment -> releases.save(new MediaReleaseEntity(null, ownerId, resourceId, attachment.id(),
                    request.releaseGroup(), request.versionLabel(), MediaReleaseState.AVAILABLE, request.contentFingerprint(),
                    Instant.now(), Instant.now(), null)));
        }).map(this::view);
    }

    @Override public Flux<MediaReleaseView> list(UUID ownerId, UUID resourceId) {
        return resources.get(ownerId, resourceId).flatMapMany(resource -> releases
            .findAllByOwnerIdAndPlayableResourceIdOrderByCreatedAtDesc(ownerId, resourceId).map(this::view));
    }

    @Override public Mono<MediaReleaseView> changeState(UUID ownerId, UUID releaseId, UpdateMediaReleaseStateRequest request) {
        return releases.findById(releaseId).filter(r -> r.ownerId().equals(ownerId))
            .switchIfEmpty(Mono.error(new NotFoundException("Media Release 不存在")))
            .flatMap(old -> {
                if (old.state() == MediaReleaseState.ARCHIVED && request.state() != MediaReleaseState.ARCHIVED) {
                    return Mono.error(new ConflictException("已归档 Release 不能恢复"));
                }
                return releases.save(new MediaReleaseEntity(old.id(), old.ownerId(), old.playableResourceId(), old.attachmentId(),
                    old.releaseGroup(), old.versionLabel(), request.state(), old.contentFingerprint(), old.createdAt(), Instant.now(), old.version()));
            }).map(this::view);
    }

    private MediaReleaseView view(MediaReleaseEntity e) { return new MediaReleaseView(e.id(), e.playableResourceId(), e.attachmentId(),
        e.releaseGroup(), e.versionLabel(), e.state(), e.contentFingerprint(), e.createdAt(), e.updatedAt()); }
}
