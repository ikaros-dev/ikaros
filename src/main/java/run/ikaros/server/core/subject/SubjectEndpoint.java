package run.ikaros.server.core.subject;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.infra.constant.OpenApiConst;
import run.ikaros.server.infra.exception.NotFoundException;

@Slf4j
@Component
public class SubjectEndpoint implements CoreEndpoint {
    private final SubjectService subjectService;

    public SubjectEndpoint(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/Subject";
        return SpringdocRouteBuilder.route()
            .GET("/subject/{id}", this::getById,
                builder -> {
                    builder
                        .operationId("SearchSubjectById")
                        .tag(tag)
                        .parameter(parameterBuilder().name("id")
                            .description("Subject ID")
                            .in(ParameterIn.PATH)
                            .required(true)
                            .implementation(Long.class))
                        .description("Search single subject by id.");
                }
            )
            .build();
    }

    private Mono<ServerResponse> getById(ServerRequest request) {
        return Mono.just(request.pathVariable("id"))
            .flatMap(id -> Mono.just(Long.valueOf(id)))
            .flatMap(subjectService::findById)
            .flatMap(subject -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subject))
            .onErrorResume(NotFoundException.class, err -> ServerResponse.notFound().build())
            .switchIfEmpty(ServerResponse.notFound().build());
    }

}
