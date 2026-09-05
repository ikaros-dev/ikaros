package run.ikaros.storage;

import java.util.List;
import java.time.Duration;
import java.util.UUID;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;
import run.ikaros.task.BackgroundTask;

/**
 * Attachment、Blob 与持久化 Placement 的公开业务能力。
 */
public interface StorageService {

    /**
     * 将一个已持久化内容登记为 Resource Attachment，并按摘要复用现有 Blob。
     *
     * @param ownerId 当前拥有者标识
     * @param resourceId Resource 标识
     * @param request 内容摘要、附件信息和持久化位置
     * @return 新建 Attachment 视图
     */
    Mono<AttachmentView> attach(UUID ownerId, UUID resourceId, AttachBlobRequest request);

    Mono<AttachmentView> commitUpload(UUID ownerId, UUID resourceId, CommitUploadRequest request);

    Mono<StorageUploadIntentView> beginUpload(UUID ownerId, UUID resourceId, BeginUploadRequest request);

    /**
     * 登记一个可追溯到原始附件的派生附件。
     *
     * @param ownerId 当前拥有者标识
     * @param resourceId Resource 标识
     * @param request 来源附件与派生内容
     * @return 新建派生附件视图
     */
    Mono<AttachmentView> attachDerived(UUID ownerId, UUID resourceId, CreateDerivedAttachmentRequest request);

    /**
     * 查询 Resource 的附件及其当前已知 Storage Placement。
     *
     * @param ownerId 当前拥有者标识
     * @param resourceId Resource 标识
     * @return Attachment 列表
     */
    Mono<List<AttachmentView>> list(UUID ownerId, UUID resourceId);

    /**
     * 按当前用户和可选 Resource 条件分页查询未归档、未删除附件。
     *
     * @param ownerId 当前拥有者标识
     * @param resourceId 可选 Resource 标识，为 null 时查询当前用户的全部附件
     * @param page 从零开始的页码
     * @param size 每页记录数
     * @return 分页附件结果
     */
    Mono<PageResponse<AttachmentView>> listPage(UUID ownerId, UUID resourceId, int page, int size);

    /**
     * 按附件身份读取元数据，并校验其所属 Resource 的访问权。
     *
     * @param ownerId 当前拥有者标识
     * @param attachmentId Attachment 标识
     * @return 附件元数据
     */
    Mono<AttachmentView> get(UUID ownerId, UUID attachmentId);

    Mono<StorageContent> readContent(UUID ownerId, UUID attachmentId, String range);

    Mono<Void> remove(UUID ownerId, UUID resourceId, UUID attachmentId);

    Mono<Void> archive(UUID ownerId, UUID resourceId, UUID attachmentId);

    /**
     * 扫描无有效 Attachment 引用的 Blob；扫描本身不执行物理删除。
     *
     * @param limit 返回上限
     * @return 可供 GC 策略继续评估的 Blob 列表
     */
    Mono<List<BlobGcCandidateView>> findGarbageCollectionCandidates(int limit, Duration minimumAge);

    /**
     * 记录 Blob GC 的人工决策，不在本步骤执行物理删除。
     *
     * @param actorId 执行决策的主体标识
     * @param blobId Blob 标识
     * @param approved 是否批准进入物理清理阶段
     * @return 审计写入完成信号
     */
    Mono<Void> recordGarbageCollectionDecision(UUID actorId, UUID blobId, boolean approved);

    Mono<BackgroundTask> requestGarbageCollection(UUID actorId, int limit, Duration minimumAge);
}
