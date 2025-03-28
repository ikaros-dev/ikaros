package run.ikaros.server.core.collection;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.store.enums.CollectionCategory;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class CollectionEndpoint implements CoreEndpoint {
    private final CollectionService collectionService;

    public CollectionEndpoint(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/collection";
        return SpringdocRouteBuilder.route()

            .GET("/collection/type/subjectId/{id}", this::getTypeBySubjectId,
                builder -> builder.operationId("GetTypeBySubjectId")
                    .tag(tag).description("Get collection type by subject id.")
                    .parameter(parameterBuilder()
                        .name("id").required(true).description("Subject id")
                        .in(ParameterIn.PATH).implementation(Long.class))
                    .response(responseBuilder()
                        .description("collection type.")
                        .implementation(CollectionType.class))
            )

            .GET("/collections/condition", this::getCollectionsWithCondition,
                builder -> builder.operationId("GetCollectionsWithCondition")
                    .tag(tag).description("Get collections with conditions.")
                    .parameter(parameterBuilder()
                        .name("category").description("Collection category")
                        .implementation(CollectionCategory.class))
                    .parameter(parameterBuilder()
                        .name("type")
                        .description("Collection type, default is not done.")
                        .implementation(CollectionType.class))
                    .parameter(parameterBuilder()
                        .name("page")
                        .description("第几页，从1开始, 默认为1.")
                        .implementation(Integer.class))
                    .parameter(parameterBuilder()
                        .name("size")
                        .description("每页条数，默认为10.")
                        .implementation(Integer.class))
                    .parameter(parameterBuilder()
                        .name("keyword")
                        .description("经过Basic64编码的关键词，"
                            + "不同类型模糊查询不同字段，一般是条目名称或者剧集名称字段。")
                        .implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("nsfw")
                        .description("Not Safe/Suitable For Work. default is false.")
                        .implementation(Boolean.class))
                    .parameter(parameterBuilder()
                        .name("time")
                        .implementation(String.class)
                        .description("时间范围，格式范围类型: 2000.9-2010.8 或者 单个类型2020.8"))
                    .parameter(parameterBuilder()
                        .name("updateTimeDesc")
                        .implementation(Boolean.class)
                        .description("是否根据更新时间倒序，默认为 true."))
                    .response(responseBuilder().implementation(PagingWrap.class))
            )

            .build();
    }

    private Mono<ServerResponse> getTypeBySubjectId(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");
        Long subjectId = Long.parseLong(id);
        return collectionService.findTypeBySubjectId(subjectId)
            .flatMap(collectionType -> ServerResponse.ok().bodyValue(collectionType));
    }

    private Mono<ServerResponse> getCollectionsWithCondition(ServerRequest serverRequest) {
        // todo impl collection search endpoint
        return Mono.empty();
    }
}
