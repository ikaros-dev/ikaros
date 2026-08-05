package run.ikaros.server.core.binding.endpoint;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import java.util.UUID;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.apiresponse.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.binding.LocalMediaMode;
import run.ikaros.api.core.binding.LocalScanConfirmRequest;
import run.ikaros.api.core.binding.LocalScanPreview;
import run.ikaros.api.core.binding.LocalScanPreviewRequest;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.binding.LocalDirectoryBindingService;
import run.ikaros.server.core.binding.service.DirectoryBindingService;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;

/** 提供远程和本地目录绑定的 HTTP 路由。 */
@Slf4j
@Component
public class DirectoryBindingEndpoint implements CoreEndpoint {

    /** 远程目录绑定服务。 */
    private final DirectoryBindingService service;
    /** 本地目录绑定服务。 */
    private final LocalDirectoryBindingService localService;

    public DirectoryBindingEndpoint(DirectoryBindingService service,
                                    LocalDirectoryBindingService localService) {
        this.service = service;
        this.localService = localService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/binding";
        return SpringdocRouteBuilder.route()

            .POST("/binding/directory", this::bindDirectory,
                builder -> builder.operationId("BindDirectory")
                    .tag(tag)
                    .description("Bind a single directory to a subject. "
                        + "Automatically finds subject, creates entries, and binds files.")
                    .parameter(parameterBuilder()
                        .name("directoryId")
                        .description("Directory attachment ID.")
                        .required(true)
                        .implementation(UUID.class))
                    .parameter(parameterBuilder()
                        .name("platform")
                        .description("Metadata platform.")
                        .required(true)
                        .implementation(SubjectSyncPlatform.class))
                    .parameter(parameterBuilder()
                        .name("platformId")
                        .description("Search platform id, overrides and "
                            + "keyword directory name if set.")
                        .required(false)
                        .implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("keyword")
                        .description("Search keyword, overrides directory name if set.")
                        .required(false)
                        .implementation(String.class))
                    .response(Builder.responseBuilder()
                        .description("Workflow entity for tracking progress.")
                        .implementation(run.ikaros.server.store.entity
                            .DirectoryBindingWorkflowEntity.class))
            )

            .POST("/binding/directories", this::bindDirectories,
                builder -> builder.operationId("BindDirectories")
                    .tag(tag)
                    .description("Batch bind all subdirectories under a parent directory.")
                    .parameter(parameterBuilder()
                        .name("parentDirectoryId")
                        .description("Parent directory attachment ID.")
                        .required(true)
                        .implementation(UUID.class))
                    .parameter(parameterBuilder()
                        .name("platform")
                        .description("Metadata platform.")
                        .required(true)
                        .implementation(SubjectSyncPlatform.class))
                    .response(Builder.responseBuilder()
                        .description("Batch binding started."))
            )

            .GET("/binding/workflow/{id}", this::getWorkflow,
                builder -> builder.operationId("GetBindingWorkflow")
                    .tag(tag)
                    .description("Get binding workflow status by workflow ID.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .in(ParameterIn.PATH)
                        .description("Workflow ID.")
                        .required(true)
                        .implementation(UUID.class))
                    .response(Builder.responseBuilder()
                        .description("Workflow entity.")
                        .implementation(run.ikaros.server.store.entity
                            .DirectoryBindingWorkflowEntity.class))
            )

            .GET("/binding/workflow/task/{taskId}", this::getWorkflowByTaskId,
                builder -> builder.operationId("GetBindingWorkflowByTaskId")
                    .tag(tag)
                    .description("Get binding workflow status by task ID.")
                    .parameter(parameterBuilder()
                        .name("taskId")
                        .in(ParameterIn.PATH)
                        .description("Task ID.")
                        .required(true)
                        .implementation(UUID.class))
                    .response(Builder.responseBuilder()
                        .description("Workflow entity.")
                        .implementation(run.ikaros.server.store.entity
                            .DirectoryBindingWorkflowEntity.class))
            )

            .POST("/binding/local/preview", this::previewLocalDirectory,
                builder -> builder.operationId("PreviewLocalDirectoryBinding")
                    .tag(tag)
                    .description("扫描本地目录并返回无副作用的媒体预览，不创建条目、剧集或工作流。")
                    .requestBody(requestBodyBuilder()
                        .description("待扫描目录的附件标识和媒体扫描模式。")
                        .required(true)
                        .implementation(LocalScanPreviewRequest.class))
                    .response(Builder.responseBuilder().responseCode("200")
                        .description("扫描成功，返回本地媒体预览。")
                        .implementation(LocalScanPreview.class))
                    .response(Builder.responseBuilder().responseCode("400")
                        .description("请求体、目录标识或扫描模式不合法。")
                        .implementation(String.class))
                    .response(Builder.responseBuilder().responseCode("404")
                        .description("指定的目录附件不存在。")
                        .implementation(String.class))
                    .response(Builder.responseBuilder().responseCode("409")
                        .description("目录扫描状态发生并发冲突。")
                        .implementation(String.class))
            )

            .POST("/binding/local/confirm", this::confirmLocalDirectory,
                builder -> builder.operationId("ConfirmLocalDirectoryBinding")
                    .tag(tag)
                    .description("确认本地扫描结果，创建或复用绑定工作流并提交后台任务。")
                    .requestBody(requestBodyBuilder()
                        .description("目录、扫描模式、唯一条目选择及待确认媒体的人工关联结果。")
                        .required(true)
                        .implementation(LocalScanConfirmRequest.class))
                    .response(Builder.responseBuilder().responseCode("200")
                        .description("确认成功，返回已提交后台任务的本地绑定工作流。")
                        .implementation(DirectoryBindingWorkflowEntity.class))
                    .response(Builder.responseBuilder().responseCode("400")
                        .description("条目选择、主资源或人工关联结果不合法。")
                        .implementation(String.class))
                    .response(Builder.responseBuilder().responseCode("404")
                        .description("指定的目录附件不存在。")
                        .implementation(String.class))
                    .response(Builder.responseBuilder().responseCode("409")
                        .description("相同本地绑定正在被并发确认。")
                        .implementation(String.class))
            )

            .POST("/binding/local/workflow/{id}/rescan", this::rescanLocalDirectory,
                builder -> builder.operationId("RescanLocalDirectoryBinding")
                    .tag(tag)
                    .description("根据已有本地绑定工作流重新扫描目录，并提交新的后台任务。")
                    .parameter(parameterBuilder()
                        .name("id")
                        .in(ParameterIn.PATH)
                        .description("要重扫的本地目录绑定工作流标识。")
                        .required(true)
                        .implementation(UUID.class))
                    .response(Builder.responseBuilder().responseCode("200")
                        .description("重扫任务提交成功，返回对应的本地绑定工作流。")
                        .implementation(DirectoryBindingWorkflowEntity.class))
                    .response(Builder.responseBuilder().responseCode("400")
                        .description("工作流标识或本地扫描状态不合法。")
                        .implementation(String.class))
                    .response(Builder.responseBuilder().responseCode("404")
                        .description("指定的工作流或其目录附件不存在。")
                        .implementation(String.class))
                    .response(Builder.responseBuilder().responseCode("409")
                        .description("该工作流正在被并发重扫。")
                        .implementation(String.class))
            )

            .build();
    }

    private Mono<ServerResponse> bindDirectory(ServerRequest request) {
        UUID directoryId = UUID.fromString(request.queryParam("directoryId").orElseThrow());
        SubjectSyncPlatform platform =
            SubjectSyncPlatform.valueOf(request.queryParam("platform").orElseThrow());
        String keyword = request.queryParam("keyword").orElse(null);
        String platformId = request.queryParam("platformId").orElse(null);
        return service.bindDirectory(directoryId, platform, keyword, platformId)
            .flatMap(workflow -> ServerResponse.ok().bodyValue(workflow));
    }

    private Mono<ServerResponse> bindDirectories(ServerRequest request) {
        UUID parentDirectoryId =
            UUID.fromString(request.queryParam("parentDirectoryId").orElseThrow());
        SubjectSyncPlatform platform =
            SubjectSyncPlatform.valueOf(request.queryParam("platform").orElseThrow());
        return service.bindDirectories(parentDirectoryId, platform)
            .then(ServerResponse.ok().bodyValue("Batch binding started."));
    }

    private Mono<ServerResponse> getWorkflow(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return service.findWorkflowById(id)
            .flatMap(wf -> ServerResponse.ok().bodyValue(wf));
    }

    private Mono<ServerResponse> getWorkflowByTaskId(ServerRequest request) {
        UUID taskId = UUID.fromString(request.pathVariable("taskId"));
        return service.findWorkflowByTaskId(taskId)
            .flatMap(wf -> ServerResponse.ok().bodyValue(wf));
    }

    private Mono<ServerResponse> previewLocalDirectory(ServerRequest request) {
        return localResponse(request.bodyToMono(LocalScanPreviewRequest.class)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("请求体不能为空")))
            .flatMap(localService::preview));
    }

    private Mono<ServerResponse> confirmLocalDirectory(ServerRequest request) {
        return localResponse(request.bodyToMono(LocalScanConfirmRequest.class)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("请求体不能为空")))
            .flatMap(localService::confirm));
    }

    private Mono<ServerResponse> rescanLocalDirectory(ServerRequest request) {
        return Mono.fromCallable(() -> UUID.fromString(request.pathVariable("id")))
            .flatMap(service::findWorkflowById)
            .flatMap(workflow -> localService.rescan(workflow.getDirectoryId(),
                workflow.getSubjectId(), LocalMediaMode.valueOf(workflow.getLocalMode())))
            .flatMap(workflow -> ServerResponse.ok().bodyValue(workflow))
            .switchIfEmpty(errorResponse(HttpStatus.NOT_FOUND, "未找到本地目录绑定工作流"))
            .onErrorResume(DuplicateKeyException.class,
                exception -> errorResponse(HttpStatus.CONFLICT, "本地绑定正在被并发处理"))
            .onErrorResume(OptimisticLockingFailureException.class,
                exception -> errorResponse(HttpStatus.CONFLICT, "本地绑定正在被并发处理"))
            .onErrorResume(IllegalArgumentException.class, this::argumentErrorResponse);
    }

    private <T> Mono<ServerResponse> localResponse(Mono<T> result) {
        return result.flatMap(value -> ServerResponse.ok().bodyValue(value))
            .onErrorResume(DuplicateKeyException.class,
                exception -> errorResponse(HttpStatus.CONFLICT, "本地绑定正在被并发处理"))
            .onErrorResume(OptimisticLockingFailureException.class,
                exception -> errorResponse(HttpStatus.CONFLICT, "本地绑定正在被并发处理"))
            .onErrorResume(IllegalArgumentException.class, this::argumentErrorResponse);
    }

    private Mono<ServerResponse> argumentErrorResponse(IllegalArgumentException exception) {
        String message = exception.getMessage();
        if ("待扫描目录附件不存在".equals(message)
            || "未找到本地目录绑定工作流".equals(message)) {
            return errorResponse(HttpStatus.NOT_FOUND, message);
        }
        return errorResponse(HttpStatus.BAD_REQUEST, message);
    }

    private Mono<ServerResponse> errorResponse(HttpStatus status, String message) {
        String readableMessage = message == null || message.isBlank()
            ? status.getReasonPhrase() : message;
        return ServerResponse.status(status).bodyValue(readableMessage);
    }
}
