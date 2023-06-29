package run.ikaros.server.core.task;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
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
        var tag = OpenApiConst.CORE_VERSION + "/Task";
        return SpringdocRouteBuilder.route()
            .GET("/task/name/{name}", this::getByName,
                builder -> builder
                    .operationId("FindTaskByName")
                    .tag(tag)
                    .parameter(Builder.parameterBuilder()
                        .name("name").required(true)
                        .in(ParameterIn.PATH))
                    .response(responseBuilder()
                        .description("Task entity.")
                        .implementation(TaskEntity.class)))
            .build();
    }

    private Mono<ServerResponse> getByName(ServerRequest request) {
        return Mono.justOrEmpty(request.pathVariable("name"))
            .flatMap(taskService::findByName)
            .flatMap(taskEntity -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(taskEntity))
            .switchIfEmpty(ServerResponse.notFound().build());
    }
}
