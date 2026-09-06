package run.ikaros.storage;

/**
 * Blob GC 人工决策请求。
 *
 * @param approved 是否批准该 Blob 进入物理清理阶段
 */
public record GarbageCollectionDecisionRequest(boolean approved) {
}
