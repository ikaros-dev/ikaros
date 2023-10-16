package run.ikaros.server.core.task;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.store.entity.TaskEntity;

@Slf4j
@Component
public class TaskEndpoint implements CoreEndpoint {
    private final TaskService taskService;

    public TaskEndpoint(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/task";
        return SpringdocRouteBuilder.route()
            .GET("/task/id/{id}", this::findById,
                builder -> builder
                    .operationId("FindTaskById")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("id").required(true)
                        .in(ParameterIn.PATH))
                    .response(responseBuilder()
                        .description("Task entity.")
                        .implementation(TaskEntity.class)))

            .GET("/task/process/{id}", this::getProcess,
                builder -> builder
                    .operationId("FindTaskProcessById")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("id").required(true)
                        .in(ParameterIn.PATH))
                    .response(responseBuilder()
                        .description("Process percentage. from 0 to 100.")
                        .implementation(Long.class)))


            .GET("/tasks/condition", this::listByCondition,
                builder -> builder.operationId("ListTasksByCondition")
                    .tag(tag).description("List tasks by condition.")
                    .parameter(parameterBuilder()
                        .name("page")
                        .description("第几页，从1开始, 默认为1.")
                        .implementation(Integer.class))
                    .parameter(parameterBuilder()
                        .name("size")
                        .description("每页条数，默认为10.")
                        .implementation(Integer.class))
                    .parameter(parameterBuilder()
                        .name("name")
                        .description("经过Basic64编码的任务名称，模糊匹配.")
                        .implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("status")
                        .description("任务状态，精准匹配.")
                        .implementation(TaskStatus.class))
                    .response(responseBuilder().implementation(PagingWrap.class))
            )


            .build();
    }

    private Mono<ServerResponse> listByCondition(ServerRequest request) {
        Optional<String> pageOp = request.queryParam("page");
        if (pageOp.isEmpty()) {
            pageOp = Optional.of("1");
        }
        final Integer page = Integer.valueOf(pageOp.get());

        Optional<String> sizeOp = request.queryParam("size");
        if (sizeOp.isEmpty()) {
            sizeOp = Optional.of("10");
        }
        final Integer size = Integer.valueOf(sizeOp.get());

        Optional<String> nameOp = request.queryParam("name");
        final String name = nameOp.isPresent() && StringUtils.hasText(nameOp.get())
            ? new String(Base64.getDecoder().decode(nameOp.get()), StandardCharsets.UTF_8)
            : "";

        Optional<String> typeOp = request.queryParam("status");
        final TaskStatus status = typeOp.isPresent() && StringUtils.hasText(typeOp.get())
            ? TaskStatus.valueOf(typeOp.get())
            : null;

        return Mono.just(FindTaskCondition.builder()
                .page(page).size(size).name(name).status(status)
                .build())
            .flatMap(taskService::listEntitiesByCondition)
            .flatMap(pagingWrap -> ServerResponse.ok().bodyValue(pagingWrap));
    }

    private Mono<ServerResponse> getProcess(ServerRequest request) {
        return Mono.justOrEmpty(request.pathVariable("id"))
            .map(Long::parseLong)
            .flatMap(taskService::getProcess)
            .flatMap(process -> ServerResponse.ok().bodyValue(process))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> findById(ServerRequest request) {
        return Mono.justOrEmpty(request.pathVariable("id"))
            .map(Long::parseLong)
            .flatMap(taskService::findById)
            .flatMap(taskEntity -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(taskEntity))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

}
