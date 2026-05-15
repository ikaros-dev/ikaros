package run.ikaros.server.core.collection;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
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
import run.ikaros.api.core.collection.SubjectCollection;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class SubjectCollectionEndpoint implements CoreEndpoint {
    private final SubjectCollectionService selfService;
    private final UserService userService;

    public SubjectCollectionEndpoint(SubjectCollectionService selfService,
                                     UserService userService) {
        this.selfService = selfService;
        this.userService = userService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/collection/subject";
        return SpringdocRouteBuilder.route()
            .GET("/collection/subjects", this::findSubjectCollections,
                builder -> builder.operationId("FindCollectionSubjects")
                    .tag(tag)
                    .description("Find user subject collections.")
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
                        .implementation(PagingWrap.class))
            )

            .GET("/collection/subject/{subjectId}", this::findSubjectCollection,
                builder -> builder.operationId("FindCollectionSubject")
                    .tag(tag)
                    .description("Find user subject collection.")
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .required(true)
                        .in(ParameterIn.PATH)
                        .implementation(String.class)
                        .description("Subject id."))
                    .response(responseBuilder()
                        .implementation(SubjectCollection.class))

            )


            .POST("/collection/subject/collect", this::collectSubject,
                builder -> builder.operationId("CollectSubject.")
                    .tag(tag)
                    .description("Collect subject by user.")
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(String.class)
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
                    .parameter(parameterBuilder()
                        .name("comment")
                        .required(false)
                        .in(ParameterIn.QUERY)
                        .implementation(String.class)
                        .description("Subject comment, with base64 encoded."))
                    .parameter(parameterBuilder()
                        .name("score")
                        .required(false)
                        .in(ParameterIn.QUERY)
                        .implementation(Integer.class)
                        .description("Subject score, from 0 to 10."))
            )

            .DELETE("/collection/subject/collect", this::unCollectSubject,
                builder -> builder.operationId("RemoveSubjectCollect.")
                    .tag(tag)
                    .description("Remove subject collect.")
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(String.class)
                        .description("Subject id."))
            )

            .PUT("/collection/subject/mainEpisodeProgress/{subjectId}/{progress}",
                this::updateSubjectCollectionMainEpProgress,
                builder -> builder.operationId("UpdateCollectionSubjectMainEpProgress")
                    .tag(tag).description("Update subject collection main episode progress.")
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .required(true)
                        .in(ParameterIn.PATH)
                        .implementation(String.class)
                        .description("Subject id."))
                    .parameter(parameterBuilder()
                        .name("progress")
                        .required(true)
                        .in(ParameterIn.PATH)
                        .implementation(Integer.class)
                        .description("Main episode progress id."))
            )


            .build();
    }

    private Mono<ServerResponse> findSubjectCollections(ServerRequest serverRequest) {
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
        return userService.getUserIdFromSecurityContext()
            .flatMap(uid -> selfService.findCollections(uid, page, size, type, isPrivate))
            .flatMap(pagingWarp -> ServerResponse.ok().bodyValue(pagingWarp))
            .switchIfEmpty(ServerResponse.notFound().build())
            .onErrorResume(NotFoundException.class,
                e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }

    private Mono<ServerResponse> findSubjectCollection(ServerRequest serverRequest) {
        UUID subjectId = UUID.fromString(serverRequest.pathVariable("subjectId"));
        return userService.getUserIdFromSecurityContext()
            .flatMap(userId -> selfService.findCollection(userId, subjectId))
            .flatMap(subjectCollection -> ServerResponse.ok().bodyValue(subjectCollection))
            .switchIfEmpty(ServerResponse.notFound().build())
            .onErrorResume(NotFoundException.class,
                e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }

    private Mono<ServerResponse> collectSubject(ServerRequest serverRequest) {
        UUID subjectId = serverRequest.queryParam("subjectId").map(UUID::fromString).orElse(null);
        Optional<String> typeOp = serverRequest.queryParam("type");
        Assert.isTrue(typeOp.isPresent(), "'type' must has value.");
        CollectionType type = CollectionType.valueOf(typeOp.get());
        Optional<String> isPrivateOp = serverRequest.queryParam("isPrivate");
        Boolean isPrivate = Boolean.valueOf(isPrivateOp.orElse(Boolean.FALSE.toString()));
        String comment = serverRequest.queryParam("comment").orElse("");
        comment = run.ikaros.api.infra.utils.StringUtils.isBase64Encoded(comment)
            ? new String(Base64.getDecoder().decode(comment), StandardCharsets.UTF_8)
            : comment;
        final String finalComment = comment;
        Integer score = Integer.valueOf(serverRequest.queryParam("score").orElse("0"));
        return userService.getUserIdFromSecurityContext()
            .flatMap(userId -> selfService.collect(SubjectCollection.builder()
                .userId(userId).type(type).subjectId(subjectId).isPrivate(isPrivate)
                .comment(finalComment).score(score)
                .build()))
            .then(ServerResponse.ok().build())
            .onErrorResume(NotFoundException.class,
                e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }

    private Mono<ServerResponse> unCollectSubject(ServerRequest serverRequest) {
        UUID subjectId = serverRequest.queryParam("subjectId").map(UUID::fromString).orElse(null);
        return userService.getUserIdFromSecurityContext()
            .flatMap(userId -> selfService.unCollect(userId, subjectId))
            .then(ServerResponse.ok().build())
            .onErrorResume(NotFoundException.class,
                e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }

    private Mono<ServerResponse> updateSubjectCollectionMainEpProgress(ServerRequest request) {
        UUID subjectId = UUID.fromString(request.pathVariable("subjectId"));
        String progressStr = request.pathVariable("progress");
        Assert.hasText(progressStr, "'progress' must has text.");
        Integer progress = Integer.valueOf(progressStr);
        return userService.getUserIdFromSecurityContext()
            .flatMap(userId -> selfService.updateMainEpisodeProgress(userId, subjectId, progress))
            .then(Mono.defer(() -> ServerResponse.ok().build()))
            .onErrorResume(NotFoundException.class,
                e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }

}
