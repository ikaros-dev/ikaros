package run.ikaros.server.core.collection;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.collection.vo.FindCollectionCondition;
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
                        .in(ParameterIn.PATH).implementation(String.class))
                    .response(responseBuilder()
                        .description("collection type.")
                        .implementation(CollectionType.class))
            )

            .GET("/collections/condition", this::getCollectionsWithCondition,
                builder -> builder.operationId("GetCollectionsWithCondition")
                    .tag(tag).description("Get collections with conditions.")
                    .parameter(parameterBuilder()
                        .name("category").description("Collection category, default is EPISODE.")
                        .implementation(CollectionCategory.class))
                    .parameter(parameterBuilder()
                        .name("type")
                        .description("Collection type, default is null.")
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
                        .name("time")
                        .implementation(String.class)
                        .description("时间范围，格式: 开始时间戳-结束时间戳"))
                    .parameter(parameterBuilder()
                        .name("updateTimeDesc")
                        .implementation(Boolean.class)
                        .description("是否根据更新时间倒序，默认为 true."))
                    .response(responseBuilder().implementation(PagingWrap.class))
            )

            .build();
    }

    private Mono<ServerResponse> getTypeBySubjectId(ServerRequest serverRequest) {
        UUID id = UUID.fromString(serverRequest.pathVariable("id"));
        return collectionService.findTypeBySubjectId(id)
            .flatMap(collectionType -> ServerResponse.ok().bodyValue(collectionType));
    }

    private Mono<ServerResponse> getCollectionsWithCondition(ServerRequest serverRequest) {
        CollectionCategory category = CollectionCategory.valueOf(
            serverRequest.queryParam("category").orElse(CollectionCategory.EPISODE.name()));
        CollectionType type = serverRequest.queryParam("type").map(CollectionType::valueOf)
            .orElse(null);
        int page = serverRequest.queryParam("page").map(Integer::parseInt).orElse(1);
        int size = serverRequest.queryParam("size").map(Integer::parseInt).orElse(10);
        String time = serverRequest.queryParam("time").orElse(null);
        boolean updateTimeDesc = serverRequest.queryParam("updateTimeDesc")
            .map(Boolean::parseBoolean).orElse(false);
        FindCollectionCondition condition = FindCollectionCondition
            .builder().page(page).size(size)
            .category(category).type(type).time(time).updateTimeDesc(updateTimeDesc)
            .build();

        return collectionService.listCollectionsByCondition(condition)
            .flatMap(pagingWrap -> ServerResponse.ok().bodyValue(pagingWrap));
    }
}
