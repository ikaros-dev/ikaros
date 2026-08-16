package run.ikaros.server.core.attachment.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.core.attachment.service.AttachmentSha1Service;

/**
 * 使用有限并发后台计算并安全回填附件 SHA-1.
 */
@Slf4j
@Service
public class DefaultAttachmentSha1Service implements AttachmentSha1Service {
    /** SHA-1 计算调度器. */
    private final Scheduler sha1Scheduler;
    /** 附件服务. */
    private final AttachmentService attachmentService;
    /** 正在计算 SHA-1 的附件 ID，用于合并重复任务. */
    private final Set<UUID> pendingAttachmentIds = ConcurrentHashMap.newKeySet();

    public DefaultAttachmentSha1Service(
        @Qualifier("attachmentSha1Scheduler") Scheduler sha1Scheduler,
        AttachmentService attachmentService) {
        this.sha1Scheduler = sha1Scheduler;
        this.attachmentService = attachmentService;
    }

    @Override
    public void calculateAsync(AttachmentDriverFetcher fetcher,
                               List<Attachment> attachments) {
        attachments.stream()
            .filter(this::isHashable)
            .filter(attachment -> pendingAttachmentIds.add(attachment.getId()))
            .forEach(attachment -> calculateAndSave(fetcher, attachment)
                .subscribeOn(sha1Scheduler)
                .doFinally(signalType -> pendingAttachmentIds.remove(attachment.getId()))
                .subscribe(null, exception -> log.warn(
                    "Attachment SHA-1 calculation failed for id={}: {}",
                    attachment.getId(), exception.getMessage())));
    }

    Mono<Void> calculateAndSave(AttachmentDriverFetcher fetcher, Attachment snapshot) {
        return fetcher.calculateSha1(snapshot)
            .flatMap(hashedAttachment -> attachmentService
                .findById(Objects.requireNonNull(snapshot.getId()))
                .filter(currentAttachment -> isSameFile(snapshot, currentAttachment))
                .map(currentAttachment ->
                    currentAttachment.setSha1(hashedAttachment.getSha1()))
                .flatMap(attachmentService::save))
            .then();
    }

    private boolean isHashable(Attachment attachment) {
        return attachment != null
            && attachment.getId() != null
            && attachment.getType() == AttachmentType.Driver_File;
    }

    private boolean isSameFile(Attachment snapshot, Attachment currentAttachment) {
        return currentAttachment.getType() == AttachmentType.Driver_File
            && !Boolean.TRUE.equals(currentAttachment.getDeleted())
            && Objects.equals(snapshot.getDriverId(), currentAttachment.getDriverId())
            && Objects.equals(snapshot.getFsPath(), currentAttachment.getFsPath())
            && Objects.equals(snapshot.getSize(), currentAttachment.getSize())
            && Objects.equals(snapshot.getModifiedTime(), currentAttachment.getModifiedTime());
    }
}
