package run.ikaros.storage.api;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** Attachment-only restore capability. Domain applications resolve business scopes before calling it. */
public interface StorageRestoreCapability {
    Mono<StorageRestoreSubmissionView> requestAttachmentSet(UUID actorId, List<UUID> attachmentIds,
        String providerRestoreClass, String budgetConfirmationToken, String idempotencyKey);
}
