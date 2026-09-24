package run.ikaros.storage;

import run.ikaros.storage.api.*;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.resource.api.ResourceOwnershipQuery;
import run.ikaros.operations.api.BackgroundTaskService;
import run.ikaros.storage.api.StorageRestoreCapability;
import run.ikaros.storage.api.StorageRestoreSubmissionView;

@Service
public class StorageRestoreRequestService implements StorageRestoreCapability {
    private static final int MAX_ATTACHMENT_SET_SIZE = 1000;
    private final AttachmentRepository attachments;
    private final ResourceOwnershipQuery resources;
    private final BlobRepository blobs;
    private final BlobPlacementRepository placements;
    private final StorageRestoreRequestRepository requests;
    private final BackgroundTaskService tasks;
    private final StorageRestoreBudgetService budget;
    private final DurableEventPublisher events;

    public StorageRestoreRequestService(AttachmentRepository attachments, ResourceOwnershipQuery resources,
        BlobRepository blobs, BlobPlacementRepository placements, StorageRestoreRequestRepository requests,
        BackgroundTaskService tasks, StorageRestoreBudgetService budget, DurableEventPublisher events) {
        this.attachments = attachments; this.resources = resources; this.blobs = blobs;
        this.placements = placements; this.requests = requests; this.tasks = tasks;
        this.budget = budget;
        this.events = events;
    }

    @Override
    public Mono<StorageRestoreSubmissionView> requestAttachmentSet(UUID actorId, List<UUID> attachmentIds,
        String providerRestoreClass, String budgetConfirmationToken, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        }
        if (attachmentIds == null || attachmentIds.isEmpty() || attachmentIds.size() > MAX_ATTACHMENT_SET_SIZE
            || attachmentIds.stream().anyMatch(java.util.Objects::isNull)) {
            return Mono.error(new IllegalArgumentException("Attachment 集合为空、超过限制或包含无效 ID"));
        }
        List<UUID> requestedIds = attachmentIds.stream().distinct().sorted().toList();
        String normalizedRestoreClass = normalizeRestoreClass(providerRestoreClass);
        String requestFingerprint = requestFingerprint(requestedIds, normalizedRestoreClass, budgetConfirmationToken);
        Mono<AttachmentSetRequest> existing = requests.findByActorIdAndScopeAndIdempotencyKey(actorId,
                StorageRestoreScope.ATTACHMENT_SET, idempotencyKey)
            .flatMap(saved -> sameFingerprint(saved, requestFingerprint)
                ? Mono.just(new AttachmentSetRequest(saved, false))
                : Mono.error(new ConflictException("Idempotency-Key 已用于不同的 Attachment 集合或恢复参数")));
        Mono<AttachmentSetRequest> created = Mono.defer(() -> Flux.fromIterable(requestedIds)
                .concatMap(id -> restoreCandidate(actorId, id))
                .collectList()
                .flatMap(candidates -> {
                    if (candidates.isEmpty()) return Mono.error(new ConflictException("Attachment 集合没有可恢复项"));
                    return selectAttachmentCandidates(candidates, budgetConfirmationToken)
                        .flatMap(selection -> {
                            Instant now = Instant.now();
                            StorageRestoreRequestStatus status = selection.budgetDecision() == StorageRestoreBudgetDecision.QUEUED
                                ? StorageRestoreRequestStatus.QUEUED : StorageRestoreRequestStatus.REQUESTED;
                            StorageRestoreRequestEntity request = new StorageRestoreRequestEntity(null, actorId,
                                StorageRestoreScope.ATTACHMENT_SET, null, status, selection.attachmentIds().size(), 0,
                                selection.totalBytes(), null, idempotencyKey, null, now, now,
                                selection.budgetDecision().name(), String.join(",", selection.attachmentIds()),
                                requestFingerprint, null);
                            return requests.save(request)
                                .map(saved -> new AttachmentSetRequest(saved, true))
                                .onErrorResume(DuplicateKeyException.class, error ->
                                    requests.findByActorIdAndScopeAndIdempotencyKey(actorId,
                                            StorageRestoreScope.ATTACHMENT_SET, idempotencyKey)
                                        .switchIfEmpty(Mono.error(error))
                                        .flatMap(saved -> sameFingerprint(saved, requestFingerprint)
                                            ? Mono.just(new AttachmentSetRequest(saved, false))
                                            : Mono.error(new ConflictException(
                                                "Idempotency-Key 已用于不同的 Attachment 集合或恢复参数"))));
                        });
                })
            );
        return existing.switchIfEmpty(created)
            .flatMap(submission -> submitAttachmentSetIfNeeded(submission.request(), normalizedRestoreClass)
                .flatMap(saved -> submission.created() ? emitRequested(Mono.just(saved)) : Mono.just(saved)))
            .map(this::view)
            .map(this::submissionView);
    }

    private Mono<RestoreCandidate> restoreCandidate(UUID actorId, UUID attachmentId) {
        return authorizedAttachment(actorId, attachmentId)
            .flatMap(attachment -> blobs.findById(attachment.blobId())
                .switchIfEmpty(Mono.error(new ConflictException("附件引用了不存在的 Blob")))
                .flatMap(blob -> placements.findAllByBlobIdOrderByCreatedAtAsc(blob.id())
                    .filter(placement -> placement.placementState() == PlacementState.ACTIVE).hasElements()
                    .flatMap(readable -> readable ? Mono.empty()
                        : Mono.just(new RestoreCandidate(attachment.id(), blob.sizeBytes())))));
    }

    private Mono<StorageRestoreRequestEntity> submitAttachmentSetIfNeeded(StorageRestoreRequestEntity saved,
        String providerRestoreClass) {
        if (saved.status() == StorageRestoreRequestStatus.QUEUED || saved.backgroundTaskId() != null) return Mono.just(saved);
        Map<String, Object> payload = Map.of("restore_request_id", saved.id().toString(),
            "selected_attachment_ids", saved.selectedAttachmentIds(), "provider_restore_class",
            providerRestoreClass);
        return tasks.submit("storage.restore", payload, "storage.restore:" + saved.id())
            .flatMap(task -> requests.save(new StorageRestoreRequestEntity(saved.id(), saved.actorId(), saved.scope(),
                saved.scopeId(), saved.status(), saved.totalItems(), saved.completedItems(), saved.totalBytes(),
                saved.errorSummary(), saved.idempotencyKey(), task.id(), saved.createdAt(), Instant.now(),
                saved.budgetDecision(), saved.selectedAttachmentIds(), saved.requestFingerprint(), saved.version()))
                .onErrorResume(OptimisticLockingFailureException.class, error -> requests.findById(saved.id())
                    .filter(current -> task.id().equals(current.backgroundTaskId()))
                    .switchIfEmpty(Mono.error(error))));
    }

    private boolean sameFingerprint(StorageRestoreRequestEntity saved, String requestFingerprint) {
        return requestFingerprint.equals(saved.requestFingerprint());
    }

    private String normalizeRestoreClass(String providerRestoreClass) {
        return providerRestoreClass == null || providerRestoreClass.isBlank() ? "STANDARD" : providerRestoreClass.trim();
    }

    private String requestFingerprint(List<UUID> attachmentIds, String restoreClass, String confirmationToken) {
        StringBuilder canonical = new StringBuilder();
        appendFingerprintField(canonical, "attachment-restore-v1");
        for (UUID attachmentId : attachmentIds) appendFingerprintField(canonical, attachmentId.toString());
        appendFingerprintField(canonical, restoreClass);
        appendFingerprintField(canonical, confirmationToken);
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private void appendFingerprintField(StringBuilder canonical, String value) {
        canonical.append(value == null ? -1 : value.length()).append(':');
        if (value != null) canonical.append(value);
        canonical.append(';');
    }

    private Mono<StorageRestorePartialSelection> selectAttachmentCandidates(List<RestoreCandidate> candidates,
        String confirmationToken) {
        return selectCandidates(candidates, confirmationToken);
    }

    private StorageRestoreSubmissionView submissionView(StorageRestoreRequestView view) {
        int failed = view.status() == StorageRestoreRequestStatus.FAILED
            || view.status() == StorageRestoreRequestStatus.PARTIAL_FAILURE
            ? Math.max(0, view.totalItems() - view.completedItems()) : 0;
        return new StorageRestoreSubmissionView(view.id(), view.scope().name(), view.scopeId(),
            status(view.status()), view.totalItems(), view.totalBytes(), view.completedItems(), failed,
            view.budgetDecision(), view.createdAt());
    }

    private String status(StorageRestoreRequestStatus value) {
        return switch (value) {
            case QUEUED, REQUESTED -> "PENDING";
            case IN_PROGRESS -> "ACTIVE";
            case COMPLETED -> "SUCCEEDED";
            case PARTIAL_FAILURE -> "PARTIAL";
            case FAILED -> "FAILED";
            case CANCELLED -> "CANCELLED";
        };
    }

    public Mono<StorageRestoreRequestView> requestAttachment(UUID actorId, RequestAttachmentRestore request,
        String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        }
        Mono<StorageRestoreRequestEntity> existing = requests.findByActorIdAndScopeAndScopeIdAndIdempotencyKey(
            actorId, StorageRestoreScope.ATTACHMENT, request.attachmentId(), idempotencyKey);
        return existing.flatMap(saved -> submitAttachmentIfNeeded(saved, request))
            .switchIfEmpty(Mono.defer(() -> authorizedAttachment(actorId, request.attachmentId())
                .flatMap(attachment -> createAttachmentRequest(actorId, attachment, request, idempotencyKey))
                .flatMap(saved -> submitAttachmentIfNeeded(saved, request))
                .flatMap(this::emitRequested)))
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
        if (saved.status() == StorageRestoreRequestStatus.QUEUED || saved.backgroundTaskId() != null) return Mono.just(saved);
        return tasks.submit("storage.restore", Map.of("restore_request_id", saved.id().toString(),
            "attachment_id", request.attachmentId().toString(), "provider_restore_class",
            request.providerRestoreClass() == null ? "STANDARD" : request.providerRestoreClass()),
            "storage.restore:" + saved.id()).flatMap(task -> requests.save(new StorageRestoreRequestEntity(
                saved.id(), saved.actorId(), saved.scope(), saved.scopeId(), saved.status(), saved.totalItems(),
                saved.completedItems(), saved.totalBytes(), saved.errorSummary(), saved.idempotencyKey(), task.id(),
                saved.createdAt(), Instant.now(), saved.budgetDecision(), saved.selectedAttachmentIds(),
                saved.requestFingerprint(), saved.version())));
    }

    private Mono<StorageRestorePartialSelection> selectCandidates(List<RestoreCandidate> candidates,
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

    public Mono<StorageRestoreRequestView> get(UUID actorId, UUID id) {
        return requests.findById(id).filter(r -> r.actorId().equals(actorId))
            .switchIfEmpty(Mono.error(new NotFoundException("Restore Request 不存在或无权访问"))).map(this::view);
    }

    public Flux<StorageRestoreRequestView> list(UUID actorId) {
        return requests.findAllByActorIdOrderByCreatedAtDesc(actorId).take(100).map(this::view);
    }

    public Flux<StorageRestoreRequestView> list(UUID actorId, StorageRestoreRequestStatus status) {
        return requests.findAllByActorIdOrderByCreatedAtDesc(actorId)
            .take(100).filter(request -> status == null || request.status() == status)
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
                    old.selectedAttachmentIds(), old.requestFingerprint(), old.version()))));
            }).map(this::view);
    }

    public Mono<StorageRestoreRequestView> retry(UUID actorId, UUID id, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("缺少 Idempotency-Key"));
        }
        return requests.findById(id).filter(request -> request.actorId().equals(actorId))
            .switchIfEmpty(Mono.error(new NotFoundException("Restore Request 不存在或无权访问")))
            .flatMap(request -> {
                if (request.status() == StorageRestoreRequestStatus.REQUESTED && request.backgroundTaskId() != null) {
                    return Mono.just(request);
                }
                if (request.status() != StorageRestoreRequestStatus.FAILED
                    && request.status() != StorageRestoreRequestStatus.PARTIAL_FAILURE) {
                    return Mono.error(new ConflictException("只有失败或部分失败的 Restore Request 可以重试"));
                }
                Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("restore_request_id", request.id().toString());
                if (request.scope() == StorageRestoreScope.ATTACHMENT) {
                    payload.put("attachment_id", request.scopeId().toString());
                } else if (request.selectedAttachmentIds() != null && !request.selectedAttachmentIds().isBlank()) {
                    payload.put("selected_attachment_ids", request.selectedAttachmentIds());
                } else {
                    return Mono.error(new ConflictException("Restore Request 缺少已冻结的 Attachment ID 集合"));
                }
                payload.put("retry_failed_only", true);
                payload.put("provider_restore_class", "STANDARD");
                return tasks.submit("storage.restore", payload, "storage.restore.retry:" + id + ":" + idempotencyKey)
                    .flatMap(task -> requests.save(new StorageRestoreRequestEntity(request.id(), request.actorId(), request.scope(),
                        request.scopeId(), StorageRestoreRequestStatus.REQUESTED, request.totalItems(), request.completedItems(),
                        request.totalBytes(), request.errorSummary(), request.idempotencyKey(), task.id(), request.createdAt(),
                        Instant.now(), request.budgetDecision(), request.selectedAttachmentIds(),
                        request.requestFingerprint(), request.version())))
                    .flatMap(updated -> events.append(new EventAppendRequest("storage.restore-request.retry-requested", 1, "storage", "restore_request", updated.id(),
                        "{\"request_id\":\"" + updated.id() + "\",\"failed_item_count\":"
                            + Math.max(0, updated.totalItems() - updated.completedItems()) + "}")).thenReturn(updated));
            }).map(this::view);
    }

    private Mono<AttachmentEntity> authorizedAttachment(UUID actorId, UUID id) {
        return attachments.findById(id).filter(a -> a.deletedAt() == null)
            .switchIfEmpty(Mono.error(new NotFoundException("附件不存在或已删除")))
            .flatMap(a -> resources.requireOwned(actorId, a.resourceId()).thenReturn(a));
    }

    private StorageRestoreRequestView view(StorageRestoreRequestEntity r) {
        return new StorageRestoreRequestView(r.id(), r.actorId(), r.scope(), r.scopeId(), r.status(), r.totalItems(),
            r.completedItems(), r.totalBytes(), r.errorSummary(), r.backgroundTaskId(), r.createdAt(), r.updatedAt(),
            r.budgetDecision() == null ? "ACCEPTED" : r.budgetDecision());
    }

    private Mono<StorageRestoreRequestEntity> emitRequested(Mono<StorageRestoreRequestEntity> saved) {
        return saved.flatMap(request -> events.append(new EventAppendRequest("storage.restore-request.requested", 1, "storage", "restore_request", request.id(),
            "{\"request_id\":\"" + request.id() + "\",\"scope_type\":\"" + request.scope()
                + "\",\"scope_id\":" + jsonUuid(request.scopeId()) + ",\"item_count\":" + request.totalItems()
                + ",\"total_bytes\":" + request.totalBytes() + ",\"budget_decision\":\"" + request.budgetDecision() + "\"}"))
            .thenReturn(request));
    }

    private String jsonUuid(UUID value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private Mono<StorageRestoreRequestEntity> emitRequested(StorageRestoreRequestEntity saved) {
        return emitRequested(Mono.just(saved));
    }

    private Mono<StorageRestoreRequestEntity> emitCancelled(Mono<StorageRestoreRequestEntity> saved) {
        return saved.flatMap(request -> events.append(new EventAppendRequest("storage.restore-request.cancel-requested", 1, "storage", "restore_request", request.id(),
            "{\"request_id\":\"" + request.id() + "\"}")).thenReturn(request));
    }

    private record RestoreCandidate(UUID attachmentId, long bytes) {}
    private record AttachmentSetRequest(StorageRestoreRequestEntity request, boolean created) {}
}
