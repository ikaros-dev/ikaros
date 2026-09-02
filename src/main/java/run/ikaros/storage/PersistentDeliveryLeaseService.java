package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PreconditionFailedException;
import run.ikaros.common.StorageUnavailableException;
import run.ikaros.resource.ResourceRepository;

@Service
public class PersistentDeliveryLeaseService implements DeliveryLeaseService {
    private static final int DEFAULT_TTL_SECONDS = 120;
    private static final int MAX_TTL_SECONDS = 1800;
    private final AttachmentRepository attachments;
    private final ResourceRepository resources;
    private final MediaDeliveryGrantRepository grants;
    private final MediaDeliveryLeaseRepository leases;
    private final BlobPlacementRepository placements;
    private final StorageProviderRegistry providerRegistry;
    private final MediaDeliveryBindingRepository bindings;

    public PersistentDeliveryLeaseService(AttachmentRepository attachments, ResourceRepository resources,
                                          MediaDeliveryGrantRepository grants, MediaDeliveryLeaseRepository leases,
                                          BlobPlacementRepository placements, StorageProviderRegistry providerRegistry,
                                          MediaDeliveryBindingRepository bindings) {
        this.attachments = attachments;
        this.resources = resources;
        this.grants = grants;
        this.leases = leases;
        this.placements = placements; this.providerRegistry = providerRegistry; this.bindings = bindings;
    }

    @Override
    public Mono<DeliveryLeaseView> create(UUID actorId, UUID attachmentId, DeliveryLeaseRequest request) {
        if (request == null || request.deliveryGrant() == null || request.deliveryGrant().isBlank())
            return Mono.error(new ConflictException("创建 Delivery Lease 必须提供有效 Grant"));
        int ttl = ttl(request.ttlSeconds());
        return grants.findByTokenHash(hash(request.deliveryGrant()))
            .filter(g -> g.attachmentId().equals(attachmentId) && g.ownerId().equals(actorId)
                && g.revokedAt() == null && g.expiresAt().isAfter(Instant.now()))
            .switchIfEmpty(Mono.error(new NotFoundException("Delivery Grant 不存在或已失效")))
            .flatMap(grant -> ownedAttachment(actorId, attachmentId).flatMap(attachment -> select(attachment.blobId()).flatMap(selection -> {
                Instant now = Instant.now();
                return leases.save(new MediaDeliveryLeaseEntity(null, attachment.id(), attachment.blobId(), actorId,
                    grant.id(), selection.bindingId(), 1, now, selection.reason(), selection.fallbackIndex(),
                    selection.healthSnapshotVersion(), now.plusSeconds(ttl), null, now, now, null)).map(this::view);
            })));
    }

    @Override
    public Mono<DeliveryLeaseView> renew(UUID actorId, UUID leaseId, Integer requestedTtl) {
        return renewInternal(actorId, leaseId, requestedTtl, null);
    }

    @Override
    public Mono<DeliveryLeaseView> renew(UUID actorId, UUID leaseId, Integer requestedTtl, long expectedVersion) {
        return renewInternal(actorId, leaseId, requestedTtl, expectedVersion);
    }

    private Mono<DeliveryLeaseView> renewInternal(UUID actorId, UUID leaseId, Integer requestedTtl,
                                                  Long expectedVersion) {
        int ttl = ttl(requestedTtl);
        Instant now = Instant.now();
        return ownedActive(actorId, leaseId).flatMap(old -> {
            checkVersion(old.version(), expectedVersion);
            return leases.save(new MediaDeliveryLeaseEntity(old.id(), old.attachmentId(), old.blobId(), old.ownerId(),
                old.grantId(), old.bindingId(), old.selectionEpoch(), old.selectedAt(), old.selectionReason(), old.fallbackIndex(),
                old.healthSnapshotVersion(), now.plusSeconds(ttl), null, now, old.createdAt(), old.version()));
        })
            .map(this::view);
    }

    @Override
    public Mono<Void> release(UUID actorId, UUID leaseId) {
        return releaseInternal(actorId, leaseId, null);
    }

    @Override
    public Mono<Void> release(UUID actorId, UUID leaseId, long expectedVersion) {
        return releaseInternal(actorId, leaseId, expectedVersion);
    }

    private Mono<Void> releaseInternal(UUID actorId, UUID leaseId, Long expectedVersion) {
        return ownedActive(actorId, leaseId).flatMap(old -> {
            checkVersion(old.version(), expectedVersion);
            return leases.save(new MediaDeliveryLeaseEntity(old.id(), old.attachmentId(), old.blobId(), old.ownerId(),
                old.grantId(), old.bindingId(), old.selectionEpoch(), old.selectedAt(), old.selectionReason(), old.fallbackIndex(),
                old.healthSnapshotVersion(), old.leaseExpiresAt(), Instant.now(), old.lastHeartbeatAt(), old.createdAt(), old.version())).then();
        });
    }

    @Override
    public Mono<Boolean> protectsBlob(UUID blobId) {
        return leases.existsByBlobIdAndReleasedAtIsNullAndLeaseExpiresAtAfter(blobId, Instant.now());
    }

    private Mono<AttachmentEntity> ownedAttachment(UUID actorId, UUID id) {
        return attachments.findById(id).filter(a -> a.deletedAt() == null)
            .flatMap(a -> resources.findByIdAndOwnerId(a.resourceId(), actorId).thenReturn(a))
            .switchIfEmpty(Mono.error(new NotFoundException("Attachment 不存在或无权访问")));
    }

    private Mono<MediaDeliveryLeaseEntity> ownedActive(UUID actorId, UUID id) {
        return leases.findByIdAndOwnerId(id, actorId)
            .filter(l -> l.releasedAt() == null && l.leaseExpiresAt().isAfter(Instant.now()))
            .switchIfEmpty(Mono.error(new NotFoundException("Delivery Lease 不存在、已释放或已过期")));
    }

    private int ttl(Integer value) {
        int result = value == null ? DEFAULT_TTL_SECONDS : value;
        if (result < 1 || result > MAX_TTL_SECONDS)
            throw new IllegalArgumentException("Lease TTL 必须在 1 到 1800 秒之间");
        return result;
    }

    private DeliveryLeaseView view(MediaDeliveryLeaseEntity l) {
        return new DeliveryLeaseView(l.id(), l.attachmentId(), l.blobId(), l.leaseExpiresAt(), l.lastHeartbeatAt(),
            l.releasedAt() == null && l.leaseExpiresAt().isAfter(Instant.now()), l.bindingId(), l.selectionEpoch(),
            l.selectedAt(), l.selectionReason(), l.fallbackIndex(), l.healthSnapshotVersion(), l.version());
    }

    private Mono<Selection> select(UUID blobId) {
        return placements.findAllByBlobIdOrderByCreatedAtAsc(blobId)
            .filter(p -> p.placementState() == PlacementState.ACTIVE)
            .concatMap(p -> providerRegistry.getByKey(p.provider())
                .filter(provider -> provider.status() != StorageProviderStatus.DISABLED
                    && provider.status() != StorageProviderStatus.FAILED)
                .flatMap(provider -> bindings.findAllByStorageProviderIdOrderByPriorityAsc(provider.id())
                    .filter(MediaDeliveryBindingEntity::enabled).next()
                    .map(binding -> new Selection(binding.id(), "PRIMARY", 0, provider.updatedAt().toString()))))
            .next()
            .switchIfEmpty(Mono.error(new StorageUnavailableException("附件没有可用 Delivery Binding")));
    }

    private record Selection(UUID bindingId, String reason, int fallbackIndex, String healthSnapshotVersion) {}

    private void checkVersion(Long actual, Long expected) {
        if (expected != null && (actual == null ? 0 : actual) != expected) {
            throw new PreconditionFailedException("If-Match 与 Delivery Lease 当前版本不匹配");
        }
    }

    private String hash(String value) {
        try {
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }
}
