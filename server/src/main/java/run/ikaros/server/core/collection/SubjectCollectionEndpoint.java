package run.ikaros.server.core.collection;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static run.ikaros.api.infra.model.ResponseResult.success;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.infra.model.ResponseResult;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class SubjectCollectionEndpoint implements CoreEndpoint {
    private final SubjectCollectionService subjectCollectionService;

    public SubjectCollectionEndpoint(SubjectCollectionService subjectCollectionService) {
        this.subjectCollectionService = subjectCollectionService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/subject/collection";
        return SpringdocRouteBuilder.route()
            .GET("/subject/collections/{userId}", this::findSubjectCollections,
                builder -> builder.operationId("FindSubjectCollections")
                    .tag(tag)
                    .description("Find user subject collections.")
                    .parameter(parameterBuilder()
                        .name("userId")
                        .required(true)
                        .in(ParameterIn.PATH)
                        .implementation(Long.class)
                        .description("User id."))
                    .parameter(parameterBuilder()
                        .name("page")
                        .required(false)
                        .in(ParameterIn.QUERY)
                        .implementation(Long.class)
                        .description("Current page, default is 1."))
                    .parameter(parameterBuilder()
                        .name("size")
                        .required(false)
                        .in(ParameterIn.QUERY)
                        .implementation(Long.class)
                        .description("Page size, default is 12."))
                    .parameter(parameterBuilder()
                        .name("type")
                        .required(false)
                        .in(ParameterIn.QUERY)
                        .implementation(CollectionType.class)
                        .description("Collection type, default is null."))
                    .parameter(parameterBuilder()
                        .name("is_private")
                        .required(false)
                        .in(ParameterIn.QUERY)
                        .implementation(Boolean.class)
                        .description("Collection is private, default is null."))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))

            .GET("/subject/collection/{userId}/{subjectId}", this::findSubjectCollection,
                builder -> builder.operationId("FindSubjectCollection")
                    .tag(tag)
                    .description("Find user subject collection.")
                    .parameter(parameterBuilder()
                        .name("userId")
                        .required(true)
                        .in(ParameterIn.PATH)
                        .implementation(Long.class)
                        .description("User id."))
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .required(true)
                        .in(ParameterIn.PATH)
                        .implementation(Long.class)
                        .description("Subject id."))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))


            .POST("/subject/collection/collect", this::collectSubject,
                builder -> builder.operationId("CollectSubject.")
                    .tag(tag)
                    .description("Collect subject by user.")
                    .parameter(parameterBuilder()
                        .name("userId")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(Long.class)
                        .description("User id."))
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(Long.class)
                        .description("Subject id."))
                    .parameter(parameterBuilder()
                        .name("type")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(CollectionType.class)
                        .description("Collection type."))
                    .parameter(parameterBuilder()
                        .name("isPrivate")
                        .required(false)
                        .in(ParameterIn.QUERY)
                        .implementation(Boolean.class)
                        .description("Is private, default is false."))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))

            .DELETE("/subject/collection/collect", this::unCollectSubject,
                builder -> builder.operationId("RemoveSubjectCollect.")
                    .tag(tag)
                    .description("Remove subject collect.")
                    .parameter(parameterBuilder()
                        .name("userId")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(Long.class)
                        .description("User id."))
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(Long.class)
                        .description("Subject id."))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))

            .PUT("/subject/collection/mainEpisodeProgress/{userId}/{subjectId}/{progress}",
                this::updateSubjectCollectionMainEpProgress,
                builder -> builder.operationId("UpdateSubjectCollectionMainEpProgress")
                    .tag(tag).description("Update subject collection main episode progress.")
                    .parameter(parameterBuilder()
                        .name("userId")
                        .required(true)
                        .in(ParameterIn.PATH)
                        .implementation(Long.class)
                        .description("User id."))
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .required(true)
                        .in(ParameterIn.PATH)
                        .implementation(Long.class)
                        .description("Subject id."))
                    .parameter(parameterBuilder()
                        .name("progress")
                        .required(true)
                        .in(ParameterIn.PATH)
                        .implementation(Integer.class)
                        .description("Main episode progress id."))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))


            .build();
    }

    private Mono<ServerResponse> findSubjectCollections(ServerRequest serverRequest) {
        String userId = serverRequest.pathVariable("userId");
        Assert.hasText(userId, "'userId' must has text.");
        Long uid = Long.valueOf(userId);
        Optional<String> pageOp = serverRequest.queryParam("page");
        Integer page = pageOp.isEmpty() || Integer.parseInt(pageOp.get()) <= 0
            ? 1 : Integer.parseInt(pageOp.get());
        Optional<String> sizeOp = serverRequest.queryParam("size");
        Integer size = (sizeOp.isEmpty() || Integer.parseInt(sizeOp.get()) <= 0)
            ? 12 : Integer.parseInt(sizeOp.get());
        Optional<String> typeOp = serverRequest.queryParam("type");
        CollectionType type = (typeOp.isPresent() && StringUtils.isNotBlank(typeOp.get()))
            ? CollectionType.valueOf(typeOp.get())
            : null;
        Optional<String> isPrivateOp = serverRequest.queryParam("is_private");
        Boolean isPrivate = isPrivateOp.map(Boolean::valueOf).orElse(null);
        return subjectCollectionService.findCollections(uid, page, size, type, isPrivate)
            .flatMap(pagingWarp -> ok().bodyValue(success(pagingWarp)));
    }

    private Mono<ServerResponse> findSubjectCollection(ServerRequest serverRequest) {
        String userIdStr = serverRequest.pathVariable("userId");
        Assert.hasText(userIdStr, "'userId' must has text.");
        Long userId = Long.valueOf(userIdStr);
        String subjectIdStr = serverRequest.pathVariable("subjectId");
        Assert.hasText(subjectIdStr, "'subjectId' must has text.");
        Long subjectId = Long.valueOf(subjectIdStr);

        return subjectCollectionService.findCollection(userId, subjectId)
            .flatMap(subjectCollection -> ok().bodyValue(success(subjectCollection)));
    }

    private Mono<ServerResponse> collectSubject(ServerRequest serverRequest) {
        Optional<String> userIdOp = serverRequest.queryParam("userId");
        Assert.isTrue(userIdOp.isPresent(), "'userId' must has value.");
        Long userId = Long.parseLong(userIdOp.get());
        Optional<String> subjectIdOp = serverRequest.queryParam("subjectId");
        Assert.isTrue(subjectIdOp.isPresent(), "'subjectId' must has value.");
        Long subjectId = Long.parseLong(subjectIdOp.get());
        Optional<String> typeOp = serverRequest.queryParam("type");
        Assert.isTrue(typeOp.isPresent(), "'type' must has value.");
        CollectionType type = CollectionType.valueOf(typeOp.get());
        Optional<String> isPrivateOp = serverRequest.queryParam("isPrivate");
        Boolean isPrivate = Boolean.valueOf(isPrivateOp.orElse(Boolean.FALSE.toString()));
        return subjectCollectionService.collect(userId, subjectId, type, isPrivate)
            .then(ok().bodyValue(success()));
    }

    private Mono<ServerResponse> unCollectSubject(ServerRequest serverRequest) {
        Optional<String> userIdOp = serverRequest.queryParam("userId");
        Assert.isTrue(userIdOp.isPresent(), "'userId' must has value.");
        Long userId = Long.parseLong(userIdOp.get());
        Optional<String> subjectIdOp = serverRequest.queryParam("subjectId");
        Assert.isTrue(subjectIdOp.isPresent(), "'subjectId' must has value.");
        Long subjectId = Long.parseLong(subjectIdOp.get());
        return subjectCollectionService.unCollect(userId, subjectId)
            .then(ok().bodyValue(success()));
    }

    private Mono<ServerResponse> updateSubjectCollectionMainEpProgress(ServerRequest request) {
        String userIdStr = request.pathVariable("userId");
        Assert.hasText(userIdStr, "'userId' must has text.");
        Long userId = Long.valueOf(userIdStr);
        String subjectIdStr = request.pathVariable("subjectId");
        Assert.hasText(subjectIdStr, "'subjectId' must has text.");
        Long subjectId = Long.valueOf(subjectIdStr);
        String progressStr = request.pathVariable("progress");
        Assert.hasText(progressStr, "'progress' must has text.");
        Integer progress = Integer.valueOf(progressStr);
        return subjectCollectionService.updateMainEpisodeProgress(userId, subjectId, progress)
            .then(Mono.defer(() -> ok().bodyValue(success())));
    }

}
