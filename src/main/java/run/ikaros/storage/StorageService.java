package run.ikaros.storage;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

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

    /**
     * 查询 Resource 的附件及其当前已知 Storage Placement。
     *
     * @param ownerId 当前拥有者标识
     * @param resourceId Resource 标识
     * @return Attachment 列表
     */
    Mono<List<AttachmentView>> list(UUID ownerId, UUID resourceId);

    /**
     * 扫描无有效 Attachment 引用的 Blob；扫描本身不执行物理删除。
     *
     * @param limit 返回上限
     * @return 可供 GC 策略继续评估的 Blob 列表
     */
    Mono<List<UUID>> findGarbageCollectionCandidates(int limit);
}
