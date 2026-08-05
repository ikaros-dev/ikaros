package run.ikaros.server.core.binding;

import java.util.UUID;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.LocalMediaMode;
import run.ikaros.api.core.binding.LocalScanConfirmRequest;
import run.ikaros.api.core.binding.LocalScanPreview;
import run.ikaros.api.core.binding.LocalScanPreviewRequest;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;

/** 本地目录绑定的预览、确认和重扫服务. */
public interface LocalDirectoryBindingService {
    /**
     * 扫描目录并返回不产生副作用的媒体预览.
     *
     * @param request 扫描目录和媒体模式
     * @return 扫描预览
     */
    Mono<LocalScanPreview> preview(LocalScanPreviewRequest request);

    /**
     * 确认扫描结果，创建或复用本地绑定工作流并提交任务.
     *
     * @param request 已确认的扫描结果
     * @return 已提交任务的本地工作流
     */
    Mono<DirectoryBindingWorkflowEntity> confirm(LocalScanConfirmRequest request);

    /**
     * 根据已确认状态重扫目录，并保留已有人工关联和剧集映射.
     *
     * @param directoryId 目录附件标识
     * @param subjectId 条目标识
     * @param mode 本地扫描模式
     * @return 已提交重扫任务的本地工作流
     */
    Mono<DirectoryBindingWorkflowEntity> rescan(UUID directoryId, UUID subjectId,
                                                LocalMediaMode mode);
}
