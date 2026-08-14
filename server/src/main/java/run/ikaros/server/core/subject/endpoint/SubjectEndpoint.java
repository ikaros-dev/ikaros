package run.ikaros.server.core.subject.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
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
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.vo.FindSubjectCondition;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.subject.service.SubjectService;
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
        var tag = OpenApiConst.CORE_VERSION + "/subject";
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
                        .implementation(Long.class))
                    .response(responseBuilder()
                        .implementation(PagingWrap.class))
            )
            .GET("/subject/{id}", this::getById,
                builder -> builder
                    .operationId("SearchSubjectById")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("id")
                        .description("Subject ID")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(String.class))
                    .description("Search single subject by id.")
                    .response(responseBuilder().implementation(Subject.class))
            )

            .GET("/subjects/condition", this::listByCondition,
                builder -> builder.operationId("ListSubjectsByCondition")
                    .tag(tag).description("按名称、关键词、类型等条件分页查询条目。")
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
                        .name("keyword")
                        .description("经过Base64编码的关键词，同时模糊匹配条目原名和中文名。")
                        .implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("nsfw")
                        .description("Not Safe/Suitable For Work. default is false.")
                        .implementation(Boolean.class))
                    .parameter(parameterBuilder()
                        .name("type")
                        .description("条目类型")
                        .implementation(SubjectType.class))
                    .parameter(parameterBuilder()
                        .name("types")
                        .description("逗号分隔的条目类型集合；去除空白并去重，非法类型返回400。")
                        .implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("time")
                        .implementation(String.class)
                        .description("时间范围，格式范围类型: 2000.9-2010.8 或者 单个类型2020.8"))
                    .parameter(parameterBuilder()
                        .name("airTimeDesc")
                        .implementation(Boolean.class)
                        .description("是否根据放送时间倒序，新番在列表前面。默认为 true."))
                    .parameter(parameterBuilder()
                        .name("updateTimeDesc")
                        .implementation(Boolean.class)
                        .description("是否根据更新时间倒序，默认为 true."))
                    .parameter(parameterBuilder()
                        .name("scoreDesc")
                        .implementation(Boolean.class)
                        .description("是否根据评分倒序，默认为空."))
                    .response(responseBuilder()
                        .description("返回符合全部筛选条件的分页条目及完整匹配数量。")
                        .implementation(PagingWrap.class))
            )

            .POST("/subject", this::create,
                builder -> builder.operationId("CreateSubject")
                    .tag(tag)
                    .description("Create single subject.")
                    .requestBody(Builder.requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_VALUE)
                            .schema(schemaBuilder().implementation(Subject.class))
                        ))
                    .response(responseBuilder().implementation(Subject.class)))

            .PUT("/subject", this::update,
                builder -> builder.operationId("UpdateSubject")
                    .tag(tag)
                    .description("Update single subject.")
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
                        .implementation(String.class)))
            .build();
    }

    private Mono<ServerResponse> listByCondition(ServerRequest request) {
        FindSubjectCondition findSubjectCondition;
        try {
            findSubjectCondition = parseFindSubjectCondition(request);
        } catch (IllegalArgumentException exception) {
            log.debug("解析条目查询参数失败。", exception);
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("message", "查询参数格式错误"));
        }
        return subjectService.listEntitiesByCondition(findSubjectCondition)
            .flatMap(pagingWrap -> ServerResponse.ok().bodyValue(pagingWrap));
    }

    private FindSubjectCondition parseFindSubjectCondition(ServerRequest request) {
        Integer page = Integer.valueOf(request.queryParam("page").orElse("1"));
        Integer size = Integer.valueOf(request.queryParam("size").orElse("10"));
        String name = decodeBase64QueryParam(request, "name");
        String nameCn = decodeBase64QueryParam(request, "nameCn");
        String keyword = decodeBase64QueryParam(request, "keyword");
        Boolean nsfw = request.queryParam("nsfw")
            .filter(StringUtils::hasText)
            .map(Boolean::valueOf)
            .orElse(null);
        SubjectType type = request.queryParam("type")
            .filter(StringUtils::hasText)
            .map(SubjectType::valueOf)
            .orElse(null);
        Set<SubjectType> types = parseSubjectTypes(request.queryParam("types").orElse(""));
        String time = request.queryParam("time").orElse("");
        boolean airTimeDesc = Boolean.parseBoolean(request.queryParam("airTimeDesc")
            .orElse(Boolean.TRUE.toString()));
        boolean updateTimeDesc = Boolean.parseBoolean(request.queryParam("updateTimeDesc")
            .orElse(Boolean.FALSE.toString()));
        Boolean scoreDesc = request.queryParam("scoreDesc")
            .filter(StringUtils::hasText)
            .map(Boolean::valueOf)
            .orElse(null);

        return FindSubjectCondition.builder()
            .page(page).size(size).name(name).nameCn(nameCn).keyword(keyword)
            .nsfw(nsfw).type(type).types(types).time(time)
            .airTimeDesc(airTimeDesc)
            .updateTimeDesc(updateTimeDesc)
            .scoreDesc(scoreDesc)
            .build();
    }

    private @Nullable String decodeBase64QueryParam(ServerRequest request, String parameterName) {
        return request.queryParam(parameterName)
            .filter(StringUtils::hasText)
            .map(value -> new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8))
            .orElse(null);
    }

    private Set<SubjectType> parseSubjectTypes(String value) {
        Set<SubjectType> types = new LinkedHashSet<>();
        for (String typeValue : value.split(",")) {
            if (StringUtils.hasText(typeValue)) {
                types.add(SubjectType.valueOf(typeValue.trim()));
            }
        }
        return types;
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
        final UUID id = UuidV7Utils.fromString(request.pathVariable("id"));
        if (id == null) {
            return ServerResponse.badRequest().bodyValue("id must be a valid UUID.");
        }
        return subjectService.findById(id)
            .flatMap(subject -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subject))
            .onErrorResume(NotFoundException.class,
                e -> ServerResponse.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(e.getMessage() == null
                        ? e.getClass().getSimpleName() : e.getMessage()))
            .switchIfEmpty(ServerResponse.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("Not found for id: " + id));
    }

    private Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(Subject.class)
            .flatMap(subjectService::create)
            .flatMap(subject -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subject));
    }

    private Mono<ServerResponse> update(ServerRequest request) {
        return request.bodyToMono(Subject.class)
            .flatMap(subjectService::update)
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> deleteById(ServerRequest request) {
        final UUID id = UuidV7Utils.fromString(request.pathVariable("id"));
        if (id == null) {
            return ServerResponse.badRequest().bodyValue("id must be a valid UUID.");
        }
        return subjectService.deleteById(id)
            .then(ServerResponse.ok().build());
    }

}
