package run.ikaros.server.core.subject;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.api.store.enums.SubjectType;
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
            .GET("/subject/{id}", this::getById,
                builder -> {
                    builder
                        .operationId("SearchSubjectById")
                        .tag(tag)
                        .parameter(parameterBuilder()
                            .name("id")
                            .description("Subject ID")
                            .in(ParameterIn.PATH)
                            .required(true)
                            .implementation(Long.class))
                        .description("Search single subject by id.");
                }
            )

            .GET("/subjects/condition", this::listByCondition,
                builder -> builder.operationId("ListSubjectsByCondition")
                    .tag(tag).description("List subjects by condition.")
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
                        .description("经过Basic64编码的名称，名称字段模糊查询。")
                        .implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("nameCn")
                        .description("经过Basic64编码的中文名称，中文名称字段模糊查询。")
                        .implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("nsfw")
                        .description("Not Safe/Suitable For Work.")
                        .implementation(Boolean.class))
                    .parameter(parameterBuilder()
                        .name("type")
                        .implementation(SubjectType.class))
                    .response(responseBuilder().implementation(PagingWrap.class))
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
            .DELETE("/subject/{id}", this::deleteById,
                builder -> builder.operationId("DeleteSubjectById")
                    .tag(tag)
                    .description("Delete subject by id.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .required(true)
                        .in(ParameterIn.PATH)
                        .description("Subject id")
                        .implementation(Long.class)))
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
            : null;
        Optional<String> nameCnOp = request.queryParam("nameCn");
        final String nameCn = nameCnOp.isPresent() && StringUtils.hasText(nameCnOp.get())
            ? new String(Base64.getDecoder().decode(nameCnOp.get()), StandardCharsets.UTF_8)
            : null;
        Optional<String> nsfwOp = request.queryParam("nsfw");
        final Boolean nsfw = nsfwOp.isPresent() && StringUtils.hasText(nsfwOp.get())
            ? Boolean.valueOf(nsfwOp.get())
            : null;
        Optional<String> typeOp = request.queryParam("type");
        final SubjectType type = typeOp.isPresent() && StringUtils.hasText(typeOp.get())
            ? SubjectType.valueOf(typeOp.get())
            : null;
        FindSubjectCondition findSubjectCondition = FindSubjectCondition.builder()
            .page(page).size(size).name(name).nameCn(nameCn)
            .nsfw(nsfw).type(type).build();
        return subjectService.listEntitiesByCondition(findSubjectCondition)
            .flatMap(pagingWrap -> ServerResponse.ok().bodyValue(pagingWrap));
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
            .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("[]"));
    }

    private Mono<ServerResponse> getById(ServerRequest request) {
        String id = request.pathVariable("id");
        return Mono.just(id)
            .map(Long::valueOf)
            .flatMap(subjectService::findById)
            .flatMap(subject -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subject))
            .onErrorResume(NotFoundException.class,
                e -> ServerResponse.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(e.getMessage()))
            .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("Not found for id: " + id));
    }

    private Mono<ServerResponse> save(ServerRequest request) {
        return request.bodyToMono(Subject.class)
            .flatMap(subjectService::save)
            .flatMap(subject -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subject));
    }

    private Mono<ServerResponse> deleteById(ServerRequest request) {
        return Mono.just(request.pathVariable("id"))
            .map(Long::valueOf)
            .flatMap(subjectService::deleteById)
            .then(ServerResponse.ok().build());
    }

}
