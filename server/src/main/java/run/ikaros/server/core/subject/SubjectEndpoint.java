package run.ikaros.server.core.subject;

import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.endpoint.CoreEndpoint;

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
            .GET("/subjects/{page}/{size}", this::list,
                builder -> builder.operationId("SearchAllSubjectByPaging")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("page")
                        .description("Search page")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class)
                        )
                    .parameter(parameterBuilder()
                        .name("size")
                        .description("Search page size")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class)))
            .GET("/subject", this::getById,
                builder -> {
                    builder
                        .operationId("SearchSubjectById")
                        .tag(tag)
                        .parameter(parameterBuilder()
                            .name("id")
                            .description("Subject ID")
                            .in(ParameterIn.QUERY)
                            .required(true)
                            .implementation(Long.class))
                        .description("Search single subject by id.");
                }
            )
            .POST("/subject", this::save,
                builder -> builder.operationId("SaveSubject")
                    .tag(tag)
                    .description("Create or update single subject.")
                    .requestBody(Builder.requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(schemaBuilder().implementation(Subject.class))
                        )))
            .DELETE("/subject", this::deleteById,
                builder -> builder.operationId("DeleteSubjectById")
                    .tag(tag)
                    .description("Delete subject by id.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .description("Subject id")
                        .implementation(Long.class)))
            .build();
    }

    private Mono<ServerResponse> list(ServerRequest request) {
        return Mono.just(request)
            .flatMap(request1 -> {
                String page = request1.pathVariable("page");
                String size = request1.pathVariable("size");
                PagingWrap<Subject> pagingWrap =
                    new PagingWrap<>(Integer.parseInt(page), Integer.parseInt(size), 0L,
                        Collections.emptyList());
                return subjectService.findAllByPageable(pagingWrap);
            })
            .filter(pagingWrap -> !pagingWrap.isEmpty())
            .flatMap(pagingWrap -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(pagingWrap))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> getById(ServerRequest request) {
        return Mono.just(request.queryParam("id").orElse("-1"))
            .map(Long::valueOf)
            .flatMap(subjectService::findById)
            .flatMap(subject -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subject))
            .onErrorResume(NotFoundException.class, err -> ServerResponse.notFound().build())
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> save(ServerRequest request) {
        return request.bodyToMono(Subject.class)
            .flatMap(subjectService::save)
            .flatMap(subject -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subject));
    }

    private Mono<ServerResponse> deleteById(ServerRequest request) {
        return Mono.just(request.queryParam("id").orElse("-1"))
            .map(Long::valueOf)
            .flatMap(subjectService::deleteById)
            .then(ServerResponse.ok().build());
    }

}
