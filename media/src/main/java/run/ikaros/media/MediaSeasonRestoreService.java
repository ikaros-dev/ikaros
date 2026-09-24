package run.ikaros.media;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.media.api.MediaRestoreTargetQuery;
import run.ikaros.storage.api.AttachmentReferenceQuery;
import run.ikaros.storage.api.StorageRestoreCapability;
import run.ikaros.storage.api.StorageRestoreSubmissionView;

/** Resolves Media business scopes before delegating frozen Attachment IDs to Storage. */
@Service
public class MediaSeasonRestoreService {
    private static final int MAX_ATTACHMENT_SET_SIZE = 1000;
    private final MediaRestoreTargetQuery targets;
    private final AttachmentReferenceQuery attachments;
    private final StorageRestoreCapability storage;

    public MediaSeasonRestoreService(MediaRestoreTargetQuery targets, AttachmentReferenceQuery attachments,
                                     StorageRestoreCapability storage) {
        this.targets = targets;
        this.attachments = attachments;
        this.storage = storage;
    }

    public Mono<StorageRestoreSubmissionView> requestSeason(UUID actorId, UUID seasonId,
        MediaSeasonRestoreOptions options, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        }
        MediaSeasonRestoreOptions request = options == null ? new MediaSeasonRestoreOptions(null, null) : options;
        return targets.requireOwnedSeason(actorId, seasonId)
            .thenMany(targets.findOwnedEpisodeResourceIds(actorId, seasonId)
                .concatMap(resourceId -> attachments.listActiveForResource(actorId, resourceId))
                .map(reference -> reference.attachmentId())
                .distinct()
                .take(MAX_ATTACHMENT_SET_SIZE + 1L))
            .collectList()
            .flatMap(attachmentIds -> {
                if (attachmentIds.size() > MAX_ATTACHMENT_SET_SIZE) {
                    return Mono.error(new ConflictException("Season Attachment 数量超过恢复请求上限"));
                }
                if (attachmentIds.isEmpty()) {
                    return Mono.error(new ConflictException("Season 没有可恢复的 Attachment"));
                }
                return storage.requestAttachmentSet(actorId, List.copyOf(attachmentIds),
                    request.providerRestoreClass(), request.budgetConfirmationToken(), idempotencyKey);
            });
    }
}
