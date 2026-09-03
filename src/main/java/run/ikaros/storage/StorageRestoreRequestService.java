package run.ikaros.storage;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.event.DurableEventService;
import run.ikaros.resource.ResourceRepository;
import run.ikaros.task.BackgroundTaskService;
import run.ikaros.media.MediaEpisodeRepository;
import run.ikaros.media.MediaSeasonRepository;

@Service
public class StorageRestoreRequestService {
    private final AttachmentRepository attachments;
    private final ResourceRepository resources;
    private final BlobRepository blobs;
    private final BlobPlacementRepository placements;
    private final StorageRestoreRequestRepository requests;
    private final BackgroundTaskService tasks;
    private final StorageRestoreBudgetService budget;
    private final MediaSeasonRepository seasons;
    private final MediaEpisodeRepository episodes;
    private final DurableEventService events;

    public StorageRestoreRequestService(AttachmentRepository attachments, ResourceRepository resources,
        BlobRepository blobs, BlobPlacementRepository placements, StorageRestoreRequestRepository requests,
        BackgroundTaskService tasks, StorageRestoreBudgetService budget, MediaSeasonRepository seasons,
        MediaEpisodeRepository episodes, DurableEventService events) {
        this.attachments = attachments; this.resources = resources; this.blobs = blobs;
        this.placements = placements; this.requests = requests; this.tasks = tasks;
        this.budget = budget;
        this.seasons = seasons; this.episodes = episodes; this.events = events;
    }

    public Mono<StorageRestoreRequestView> requestAttachment(UUID actorId, RequestAttachmentRestore request,
        String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        }
        Mono<StorageRestoreRequestEntity> existing = requests.findByActorIdAndScopeAndScopeIdAndIdempotencyKey(
            actorId, StorageRestoreScope.ATTACHMENT, request.attachmentId(), idempotencyKey);
        return existing.switchIfEmpty(Mono.defer(() -> authorizedAttachment(actorId, request.attachmentId())
            .flatMap(attachment -> createAttachmentRequest(actorId, attachment, request, idempotencyKey))))
            .flatMap(saved -> submitAttachmentIfNeeded(saved, request))
            .flatMap(this::emitRequested)
            .map(this::view);
    }

    private Mono<StorageRestoreRequestEntity> createAttachmentRequest(UUID actorId, AttachmentEntity attachment,
        RequestAttachmentRestore request, String idempotencyKey) {
        return blobs.findById(attachment.blobId())
            .switchIfEmpty(Mono.error(new ConflictException("附件引用了不存在的 Blob")))
            .flatMap(blob -> budget.evaluate(1, blob.sizeBytes(), request.budgetConfirmationToken())
                .flatMap(decision -> placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                    .filter(p -> p.placementState() == PlacementState.ACTIVE).hasElements()
                    .flatMap(readable -> {
                        if (readable) return Mono.error(new ConflictException("附件已经存在可读副本"));
                        Instant now = Instant.now();
                        return requests.save(new StorageRestoreRequestEntity(null, actorId, StorageRestoreScope.ATTACHMENT,
                            request.attachmentId(), decision == StorageRestoreBudgetDecision.QUEUED
                                ? StorageRestoreRequestStatus.QUEUED : StorageRestoreRequestStatus.REQUESTED,
                            1, 0, blob.sizeBytes(), null, idempotencyKey, null, now, now, decision.name(), null));
                    })));
    }

    private Mono<StorageRestoreRequestEntity> submitAttachmentIfNeeded(StorageRestoreRequestEntity saved,
        RequestAttachmentRestore request) {
        if (saved.status() == StorageRestoreRequestStatus.QUEUED) return Mono.just(saved);
        return tasks.submit("storage.restore", Map.of("restore_request_id", saved.id().toString(),
            "attachment_id", request.attachmentId().toString(), "provider_restore_class",
            request.providerRestoreClass() == null ? "STANDARD" : request.providerRestoreClass()),
            "storage.restore:" + saved.id()).flatMap(task -> requests.save(new StorageRestoreRequestEntity(
                saved.id(), saved.actorId(), saved.scope(), saved.scopeId(), saved.status(), saved.totalItems(),
                saved.completedItems(), saved.totalBytes(), saved.errorSummary(), saved.idempotencyKey(), task.id(),
                saved.createdAt(), Instant.now(), saved.budgetDecision(), saved.selectedAttachmentIds(), saved.version())));
    }

    public Mono<StorageRestoreRequestView> requestSeason(UUID actorId, UUID seasonId, String providerRestoreClass,
        String idempotencyKey) {
        return requestSeason(actorId, seasonId, providerRestoreClass, null, idempotencyKey);
    }

    public Mono<StorageRestoreRequestView> requestSeason(UUID actorId, UUID seasonId, String providerRestoreClass,
        String budgetConfirmationToken, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        return requests.findByActorIdAndScopeAndScopeIdAndIdempotencyKey(actorId, StorageRestoreScope.SEASON, seasonId, idempotencyKey)
            .switchIfEmpty(Mono.defer(() -> createSeasonRequest(actorId, seasonId, budgetConfirmationToken, idempotencyKey)))
            .flatMap(saved -> submitSeasonIfNeeded(saved, seasonId, providerRestoreClass))
            .flatMap(this::emitRequested)
            .map(this::view);
    }

    private Mono<StorageRestoreRequestEntity> createSeasonRequest(UUID actorId, UUID seasonId,
        String budgetConfirmationToken, String idempotencyKey) {
        return seasons.findById(seasonId).filter(s -> s.ownerId().equals(actorId))
            .switchIfEmpty(Mono.error(new NotFoundException("Season 不存在或无权访问")))
            .thenMany(episodes.findAllByOwnerIdAndSeasonIdOrderByEpisodeNumberAsc(actorId, seasonId))
            .flatMap(e -> attachments.findAllByResourceIdAndDeletedAtIsNullOrderByCreatedAtAsc(e.resourceId()))
            .flatMap(a -> blobs.findById(a.blobId()).flatMap(blob -> placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                .filter(p -> p.placementState() == PlacementState.ACTIVE).hasElements()
                .flatMap(readable -> readable ? Mono.empty() : Mono.just(new RestoreCandidate(a.id(), blob.sizeBytes())))))
            .collectList()
            .flatMap(candidates -> {
                if (candidates.isEmpty()) return Mono.error(new ConflictException("Season 没有可恢复附件"));
                return selectSeasonCandidates(candidates, budgetConfirmationToken).flatMap(selection -> {
                        Instant now = Instant.now();
                        StorageRestoreRequestStatus status = selection.budgetDecision() == StorageRestoreBudgetDecision.QUEUED
                            ? StorageRestoreRequestStatus.QUEUED : StorageRestoreRequestStatus.REQUESTED;
                        return requests.save(new StorageRestoreRequestEntity(null, actorId, StorageRestoreScope.SEASON, seasonId,
                            status, selection.attachmentIds().size(), 0, selection.totalBytes(), null, idempotencyKey, null,
                            now, now, selection.budgetDecision().name(), String.join(",", selection.attachmentIds()), null));
                    });
            });
    }

    private Mono<StorageRestorePartialSelection> selectSeasonCandidates(List<RestoreCandidate> candidates,
        String confirmationToken) {
        long totalBytes = candidates.stream().mapToLong(RestoreCandidate::bytes).sum();
        return budget.get().flatMap(policy -> {
            if (policy.overBudgetAction() != StorageRestoreBudgetAction.PARTIAL_ACCEPT
                || (candidates.size() <= policy.maxItemsPerRequest() && totalBytes <= policy.maxBytesPerRequest())) {
                return budget.evaluate(candidates.size(), totalBytes, confirmationToken)
                    .map(decision -> new StorageRestorePartialSelection(candidates.stream().map(c -> c.attachmentId().toString()).toList(),
                        totalBytes, decision));
            }
            List<String> selected = new java.util.ArrayList<>();
            long selectedBytes = 0;
            for (RestoreCandidate candidate : candidates) {
                if (selected.size() >= policy.maxItemsPerRequest()) break;
                if (candidate.bytes() > policy.maxBytesPerRequest() - selectedBytes) continue;
                selected.add(candidate.attachmentId().toString());
                selectedBytes += candidate.bytes();
            }
            if (selected.isEmpty()) return Mono.error(new ConflictException("没有附件落在当前预算内"));
            final long acceptedBytes = selectedBytes;
            return budget.evaluate(selected.size(), acceptedBytes, confirmationToken)
                .map(decision -> new StorageRestorePartialSelection(selected, acceptedBytes,
                    StorageRestoreBudgetDecision.PARTIAL));
        });
    }

    private Mono<StorageRestoreRequestEntity> submitSeasonIfNeeded(StorageRestoreRequestEntity saved, UUID seasonId,
        String providerRestoreClass) {
        if (saved.status() == StorageRestoreRequestStatus.QUEUED) return Mono.just(saved);
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("restore_request_id", saved.id().toString());
        payload.put("season_id", seasonId.toString());
        payload.put("provider_restore_class", providerRestoreClass == null ? "STANDARD" : providerRestoreClass);
        if (saved.selectedAttachmentIds() != null && !saved.selectedAttachmentIds().isBlank()) {
            payload.put("selected_attachment_ids", saved.selectedAttachmentIds());
        }
        return tasks.submit("storage.restore", payload, "storage.restore:" + saved.id())
            .flatMap(task -> requests.save(new StorageRestoreRequestEntity(saved.id(), saved.actorId(), saved.scope(),
                saved.scopeId(), saved.status(), saved.totalItems(), saved.completedItems(), saved.totalBytes(),
                saved.errorSummary(), saved.idempotencyKey(), task.id(), saved.createdAt(), Instant.now(),
                saved.budgetDecision(), saved.selectedAttachmentIds(), saved.version())));
    }

    public Mono<StorageRestoreRequestView> get(UUID actorId, UUID id) {
        return requests.findById(id).filter(r -> r.actorId().equals(actorId))
            .switchIfEmpty(Mono.error(new NotFoundException("Restore Request 不存在或无权访问"))).map(this::view);
    }

    public Flux<StorageRestoreRequestView> list(UUID actorId) {
        return requests.findAllByActorIdOrderByCreatedAtDesc(actorId).map(this::view);
    }

    public Flux<StorageRestoreRequestView> list(UUID actorId, StorageRestoreRequestStatus status) {
        return requests.findAllByActorIdOrderByCreatedAtDesc(actorId)
            .filter(request -> status == null || request.status() == status)
            .map(this::view);
    }

    public Mono<RestoreRequestPage> listPage(UUID actorId, StorageRestoreRequestStatus status, String cursor) {
        return list(actorId, status).collectList().map(all -> {
            int start = 0;
            if (cursor != null && !cursor.isBlank()) {
                UUID cursorId;
                try {
                    cursorId = UUID.fromString(cursor);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Restore Request cursor 无效");
                }
                int cursorIndex = -1;
                for (int i = 0; i < all.size(); i++) {
                    if (all.get(i).id().equals(cursorId)) { cursorIndex = i; break; }
                }
                if (cursorIndex < 0) throw new IllegalArgumentException("Restore Request cursor 不属于当前查询");
                start = cursorIndex + 1;
            }
            int end = Math.min(start + 50, all.size());
            String next = end < all.size() ? all.get(end - 1).id().toString() : null;
            return new RestoreRequestPage(all.subList(start, end), next);
        });
    }

    public Mono<StorageRestoreRequestView> cancel(UUID actorId, UUID id) {
        return requests.findById(id).filter(r -> r.actorId().equals(actorId))
            .switchIfEmpty(Mono.error(new NotFoundException("Restore Request 不存在或无权访问")))
            .flatMap(old -> {
                if (old.status() == StorageRestoreRequestStatus.COMPLETED
                    || old.status() == StorageRestoreRequestStatus.FAILED
                    || old.status() == StorageRestoreRequestStatus.PARTIAL_FAILURE
                    || old.status() == StorageRestoreRequestStatus.CANCELLED) return Mono.just(old);
                Mono<Void> stop = old.backgroundTaskId() == null ? Mono.empty() : tasks.cancel(old.backgroundTaskId()).then();
                return stop.then(emitCancelled(requests.save(new StorageRestoreRequestEntity(old.id(), old.actorId(), old.scope(), old.scopeId(),
                    StorageRestoreRequestStatus.CANCELLED, old.totalItems(), old.completedItems(), old.totalBytes(), old.errorSummary(),
                    old.idempotencyKey(), old.backgroundTaskId(), old.createdAt(), Instant.now(), old.budgetDecision(),
                    old.selectedAttachmentIds(), old.version()))));
            }).map(this::view);
    }

    public Mono<StorageRestoreRequestView> retry(UUID actorId, UUID id, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        }
        return requests.findById(id).filter(request -> request.actorId().equals(actorId))
            .switchIfEmpty(Mono.error(new NotFoundException("Restore Request 不存在或无权访问")))
            .flatMap(request -> {
                if (request.status() != StorageRestoreRequestStatus.FAILED
                    && request.status() != StorageRestoreRequestStatus.PARTIAL_FAILURE) {
                    return Mono.error(new ConflictException("只有失败或部分失败的 Restore Request 可以重试"));
                }
                Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("restore_request_id", request.id().toString());
                payload.put(request.scope() == StorageRestoreScope.SEASON ? "season_id" : "attachment_id",
                    request.scopeId().toString());
                payload.put("retry_failed_only", true);
                payload.put("provider_restore_class", "STANDARD");
                if (request.selectedAttachmentIds() != null && !request.selectedAttachmentIds().isBlank()) {
                    payload.put("selected_attachment_ids", request.selectedAttachmentIds());
                }
                return tasks.submit("storage.restore", payload, "storage.restore.retry:" + id + ":" + idempotencyKey)
                    .flatMap(task -> requests.save(new StorageRestoreRequestEntity(request.id(), request.actorId(), request.scope(),
                        request.scopeId(), StorageRestoreRequestStatus.REQUESTED, request.totalItems(), request.completedItems(),
                        request.totalBytes(), request.errorSummary(), request.idempotencyKey(), task.id(), request.createdAt(),
                        Instant.now(), request.budgetDecision(), request.selectedAttachmentIds(), request.version())))
                    .flatMap(updated -> events.append("storage.restore-request.retry-requested", 1, "restore_request", updated.id(),
                        "{\"request_id\":\"" + updated.id() + "\",\"failed_item_count\":"
                            + Math.max(0, updated.totalItems() - updated.completedItems()) + "}").thenReturn(updated));
            }).map(this::view);
    }

    private Mono<AttachmentEntity> authorizedAttachment(UUID actorId, UUID id) {
        return attachments.findById(id).filter(a -> a.deletedAt() == null)
            .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或已删除")))
            .flatMap(a -> resources.findByIdAndOwnerId(a.resourceId(), actorId)
                .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或无权访问"))).thenReturn(a));
    }

    private StorageRestoreRequestView view(StorageRestoreRequestEntity r) {
        return new StorageRestoreRequestView(r.id(), r.actorId(), r.scope(), r.scopeId(), r.status(), r.totalItems(),
            r.completedItems(), r.totalBytes(), r.errorSummary(), r.backgroundTaskId(), r.createdAt(), r.updatedAt(),
            r.budgetDecision() == null ? "ACCEPTED" : r.budgetDecision());
    }

    private Mono<StorageRestoreRequestEntity> emitRequested(Mono<StorageRestoreRequestEntity> saved) {
        return saved.flatMap(request -> events.append("storage.restore-request.requested", 1, "restore_request", request.id(),
            "{\"request_id\":\"" + request.id() + "\",\"scope_type\":\"" + request.scope()
                + "\",\"scope_id\":\"" + request.scopeId() + "\",\"item_count\":" + request.totalItems()
                + ",\"total_bytes\":" + request.totalBytes() + ",\"budget_decision\":\"" + request.budgetDecision() + "\"}")
            .thenReturn(request));
    }

    private Mono<StorageRestoreRequestEntity> emitRequested(StorageRestoreRequestEntity saved) {
        return emitRequested(Mono.just(saved));
    }

    private Mono<StorageRestoreRequestEntity> emitCancelled(Mono<StorageRestoreRequestEntity> saved) {
        return saved.flatMap(request -> events.append("storage.restore-request.cancel-requested", 1, "restore_request", request.id(),
            "{\"request_id\":\"" + request.id() + "\"}").thenReturn(request));
    }

    private record RestoreCandidate(UUID attachmentId, long bytes) {}
}
