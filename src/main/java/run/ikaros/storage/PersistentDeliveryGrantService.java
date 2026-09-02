package run.ikaros.storage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.InvalidRangeException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PreconditionFailedException;
import run.ikaros.resource.ResourceRepository;

@Service
public class PersistentDeliveryGrantService implements DeliveryGrantService {
    private static final int DEFAULT_TTL_SECONDS = 300;
    private static final int MAX_TTL_SECONDS = 3600;
    private final AttachmentRepository attachments;
    private final ResourceRepository resources;
    private final MediaDeliveryGrantRepository grants;
    private final SecureRandom random = new SecureRandom();

    public PersistentDeliveryGrantService(AttachmentRepository attachments, ResourceRepository resources,
                                          MediaDeliveryGrantRepository grants) {
        this.attachments = attachments;
        this.resources = resources;
        this.grants = grants;
    }

    @Override
    public Mono<DeliveryGrantView> issue(UUID actorId, UUID attachmentId, DeliveryGrantRequest request) {
        DeliveryGrantRequest value = request == null ? new DeliveryGrantRequest(null, null, null) : request;
        int ttl = value.ttlSeconds() == null ? DEFAULT_TTL_SECONDS : value.ttlSeconds();
        if (ttl < 1 || ttl > MAX_TTL_SECONDS) return Mono.error(new IllegalArgumentException("Grant TTL 必须在 1 到 3600 秒之间"));
        validateRange(value.rangeStart(), value.rangeEnd());
        return ownedAttachment(actorId, attachmentId).flatMap(attachment -> {
            String token = newToken();
            Instant now = Instant.now();
            MediaDeliveryGrantEntity entity = new MediaDeliveryGrantEntity(null, attachment.id(), actorId,
                hash(token), "GET", value.rangeStart(), value.rangeEnd(), now.plusSeconds(ttl),
                DeliveryGrantRevocationLevel.IMMEDIATE, null, now, null);
            return grants.save(entity).map(saved -> new DeliveryGrantView(saved.id(), saved.attachmentId(), token,
                saved.method(), saved.expiresAt(), saved.rangeStart(), saved.rangeEnd(), saved.revocationLevel(), saved.version()));
        });
    }

    @Override
    public Mono<UUID> authorize(UUID actorId, UUID attachmentId, String token, String range) {
        if (token == null || token.isBlank()) return Mono.error(new NotFoundException("Delivery Grant 不存在或已失效"));
        return grants.findByTokenHash(hash(token))
            .filter(g -> g.attachmentId().equals(attachmentId) && g.revokedAt() == null && g.expiresAt().isAfter(Instant.now()))
            .switchIfEmpty(Mono.error(new NotFoundException("Delivery Grant 不存在或已失效")))
            .flatMap(g -> {
                if (actorId != null && !g.ownerId().equals(actorId)) return Mono.error(new NotFoundException("Delivery Grant 不存在或已失效"));
                checkRequestedRange(g, range);
                return Mono.just(g.ownerId());
            });
    }

    @Override
    public Mono<Void> revoke(UUID actorId, UUID grantId) {
        return revokeInternal(actorId, grantId, null);
    }

    @Override
    public Mono<Void> revoke(UUID actorId, UUID grantId, long expectedVersion) {
        return revokeInternal(actorId, grantId, expectedVersion);
    }

    private Mono<Void> revokeInternal(UUID actorId, UUID grantId, Long expectedVersion) {
        return grants.findByIdAndOwnerId(grantId, actorId)
            .switchIfEmpty(Mono.error(new NotFoundException("Delivery Grant 不存在或无权访问")))
            .flatMap(g -> {
                long actualVersion = g.version() == null ? 0 : g.version();
                if (expectedVersion != null && actualVersion != expectedVersion) {
                    return Mono.error(new PreconditionFailedException("If-Match 与 Delivery Grant 当前版本不匹配"));
                }
                return g.revocationLevel() == DeliveryGrantRevocationLevel.IMMEDIATE
                ? grants.save(new MediaDeliveryGrantEntity(g.id(), g.attachmentId(), g.ownerId(), g.tokenHash(), g.method(),
                    g.rangeStart(), g.rangeEnd(), g.expiresAt(), g.revocationLevel(), Instant.now(), g.createdAt(), g.version())).then()
                : Mono.error(new ConflictException("当前 Provider 不支持 Grant 立即撤销"));
            });
    }

    private Mono<AttachmentEntity> ownedAttachment(UUID actorId, UUID id) {
        return attachments.findById(id).filter(a -> a.deletedAt() == null)
            .flatMap(a -> resources.findByIdAndOwnerId(a.resourceId(), actorId).thenReturn(a))
            .switchIfEmpty(Mono.error(new NotFoundException("Attachment 不存在或无权访问")));
    }

    private void checkRequestedRange(MediaDeliveryGrantEntity grant, String range) {
        if (range == null || range.isBlank() || grant.rangeStart() == null) return;
        long[] requested = parseRange(range);
        if (requested[0] < grant.rangeStart() || (grant.rangeEnd() != null && requested[1] > grant.rangeEnd()))
            throw new InvalidRangeException("请求 Range 超出 Delivery Grant 授权范围");
    }

    private long[] parseRange(String range) {
        if (!range.startsWith("bytes=") || range.indexOf(',') >= 0) throw new InvalidRangeException("仅支持单一 bytes Range");
        String[] parts = range.substring(6).split("-", -1);
        try {
            if (parts.length != 2 || parts[0].isBlank()) throw new NumberFormatException();
            long start = Long.parseLong(parts[0]);
            long end = parts[1].isBlank() ? Long.MAX_VALUE : Long.parseLong(parts[1]);
            if (start < 0 || end < start) throw new NumberFormatException();
            return new long[] {start, end};
        } catch (NumberFormatException ex) {
            throw new InvalidRangeException("Range 格式无效");
        }
    }

    private void validateRange(Long start, Long end) {
        if ((start != null && start < 0) || (end != null && end < 0) || (start != null && end != null && start > end))
            throw new InvalidRangeException("Grant Range 范围无效");
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }
}
