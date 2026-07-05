package run.ikaros.api.core.attachment;

import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.IkarosExtensionPoint;

/**
 * 插件附件访问地址提供者扩展点.
 * 插件实现此接口来为指定驱动提供带条件的附件访问地址（如网盘VIP播放直链）.
 */
public interface AttachmentAccessUrlProvider extends IkarosExtensionPoint {

    /**
     * 判断此提供者是否支持处理指定附件（按驱动匹配）.
     */
    boolean supports(Attachment attachment);

    /**
     * 根据附件和条件参数生成访问地址.
     *
     * @param attachment 附件
     * @param conditions 条件参数，如 {"quality":"4k","vipToken":"xxx"}
     * @return 访问地址
     */
    Mono<String> getAccessUrl(Attachment attachment, Map<String, Object> conditions);

    /**
     * 返回此提供者支持的条件参数定义列表.
     * 用于服务端和前端展示可传入的参数信息.
     */
    default List<AccessUrlCondition> getConditionDefinitions() {
        return List.of();
    }
}
