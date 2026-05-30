package run.ikaros.server.core.binding.endpoint;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.apiresponse.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.binding.service.DirectoryBindingService;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class DirectoryBindingEndpoint implements CoreEndpoint {

    private final DirectoryBindingService service;

    public DirectoryBindingEndpoint(DirectoryBindingService service) {
        this.service = service;
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
                        .description("Task ID.")
                        .required(true)
                        .implementation(UUID.class))
                    .response(Builder.responseBuilder()
                        .description("Workflow entity.")
                        .implementation(run.ikaros.server.store.entity
                            .DirectoryBindingWorkflowEntity.class))
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
}
