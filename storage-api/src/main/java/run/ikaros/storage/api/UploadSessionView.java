package run.ikaros.storage.api;

import java.time.Instant;
import java.util.UUID;

/** 上传会话的公开状态，不包含 Provider 凭据或临时认证材料。 */
public record UploadSessionView(UUID id, UUID ownerId, UUID resourceId, String provider, String objectKey,
                                long expectedSize, String declaredSha256, UploadSessionState state,
                                Instant expiresAt, Instant createdAt, Instant updatedAt, Long version) { }
