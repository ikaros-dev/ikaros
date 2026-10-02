package run.ikaros.resource.api;

import java.util.UUID;

/**
 * 资源库浏览与筛选条件。
 *
 * <p>筛选维度对应 Console IA 契约 §6.1。Availability 需要 storage 侧的可服务性数据，
 * 属于跨模块能力，尚未纳入本查询；引入前需先补契约与 ADR。</p>
 *
 * @param type Resource 类型过滤，{@code null} 表示不过滤
 * @param keyword 标题关键词，空白表示不过滤
 * @param lifecycle 生命周期过滤，{@code null} 表示不过滤
 * @param collectionId Collection 过滤，{@code null} 表示不过滤
 * @param tag 标签名精确过滤，空白表示不过滤
 * @param sourceProvider 外部身份 provider 过滤，空白表示不过滤
 * @param page 从零开始的页码
 * @param size 每页记录数
 */
public record ResourceLibraryQuery(ResourceType type, String keyword, ResourceLifecycle lifecycle,
                                   UUID collectionId, String tag, String sourceProvider,
                                   int page, int size) {

    /** 只按类型、关键词和生命周期浏览。 */
    public static ResourceLibraryQuery of(ResourceType type, String keyword, ResourceLifecycle lifecycle,
                                          int page, int size) {
        return new ResourceLibraryQuery(type, keyword, lifecycle, null, null, null, page, size);
    }
}
