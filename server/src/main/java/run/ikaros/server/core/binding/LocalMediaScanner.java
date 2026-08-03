package run.ikaros.server.core.binding;

import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.LocalScanPreview;
import run.ikaros.api.core.binding.LocalScanPreviewRequest;

/** 扫描已收录本地附件树并生成无副作用的媒体分类预览。 */
public interface LocalMediaScanner {

    /**
     * 对请求目录执行受限的只读媒体扫描。
     *
     * @param request 包含目录附件和媒体模式的请求
     * @return 确定性排序后的扫描预览
     */
    Mono<LocalScanPreview> scan(LocalScanPreviewRequest request);
}
