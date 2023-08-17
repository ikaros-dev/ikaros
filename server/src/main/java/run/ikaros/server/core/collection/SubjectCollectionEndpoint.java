package run.ikaros.server.core.collection;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.infra.exception.NotFoundException;
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
        var tag = OpenApiConst.CORE_VERSION + "/Collection/Subject";
        return SpringdocRouteBuilder.route()
            .GET("/collection/subject/{userId}", this::findUserSubjectCollections,
                builder -> builder.operationId("FindUserSubjectCollections")
                    .tag(tag)
                    .description("Find user subject collections.")
                    .parameter(Builder.parameterBuilder()
                        .name("userId")
                        .required(true)
                        .in(ParameterIn.PATH)
                        .implementation(Long.class)
                        .description("User id."))
                    .parameter(Builder.parameterBuilder()
                        .name("page")
                        .required(false)
                        .in(ParameterIn.QUERY)
                        .implementation(Long.class)
                        .description("Current page, default is 1."))
                    .parameter(Builder.parameterBuilder()
                        .name("size")
                        .required(false)
                        .in(ParameterIn.QUERY)
                        .implementation(Long.class)
                        .description("Page size, default is 12."))
                    .parameter(Builder.parameterBuilder()
                        .name("type")
                        .required(false)
                        .in(ParameterIn.QUERY)
                        .implementation(CollectionType.class)
                        .description("Collection type, default is null."))
                    .parameter(Builder.parameterBuilder()
                        .name("is_private")
                        .required(false)
                        .in(ParameterIn.QUERY)
                        .implementation(Boolean.class)
                        .description("Collection is private, default is null."))
            )

            .POST("/collection/subject/collect", this::collectSubject,
                builder -> builder.operationId("CollectSubject.")
                    .tag(tag)
                    .description("Collect subject by user.")
                    .parameter(Builder.parameterBuilder()
                        .name("userId")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(Long.class)
                        .description("User id."))
                    .parameter(Builder.parameterBuilder()
                        .name("subjectId")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(Long.class)
                        .description("Subject id."))
                    .parameter(Builder.parameterBuilder()
                        .name("type")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(CollectionType.class)
                        .description("Collection type."))
                    .parameter(Builder.parameterBuilder()
                        .name("isPrivate")
                        .required(false)
                        .in(ParameterIn.QUERY)
                        .implementation(Boolean.class)
                        .description("Is private, default is false."))
            )

            .DELETE("/collection/subject/collect", this::unCollectSubject,
                builder -> builder.operationId("RemoveSubjectCollect.")
                    .tag(tag)
                    .description("Remove subject collect.")
                    .parameter(Builder.parameterBuilder()
                        .name("userId")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(Long.class)
                        .description("User id."))
                    .parameter(Builder.parameterBuilder()
                        .name("subjectId")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(Long.class)
                        .description("Subject id."))
            )


            .build();
    }

    private Mono<ServerResponse> findUserSubjectCollections(ServerRequest serverRequest) {
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
        CollectionType type = typeOp.map(CollectionType::valueOf).orElse(null);
        Optional<String> isPrivateOp = serverRequest.queryParam("is_private");
        Boolean isPrivate = isPrivateOp.map(Boolean::valueOf).orElse(null);
        return subjectCollectionService.findUserCollections(uid, page, size, type, isPrivate)
            .flatMap(pagingWarp -> ServerResponse.ok().bodyValue(pagingWarp))
            .switchIfEmpty(ServerResponse.notFound().build())
            .onErrorResume(NotFoundException.class,
                e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
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
            .then(ServerResponse.ok().build())
            .onErrorResume(NotFoundException.class,
                e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }

    private Mono<ServerResponse> unCollectSubject(ServerRequest serverRequest) {
        Optional<String> userIdOp = serverRequest.queryParam("userId");
        Assert.isTrue(userIdOp.isPresent(), "'userId' must has value.");
        Long userId = Long.parseLong(userIdOp.get());
        Optional<String> subjectIdOp = serverRequest.queryParam("subjectId");
        Assert.isTrue(subjectIdOp.isPresent(), "'subjectId' must has value.");
        Long subjectId = Long.parseLong(subjectIdOp.get());
        return subjectCollectionService.unCollect(userId, subjectId)
            .then(ServerResponse.ok().build())
            .onErrorResume(NotFoundException.class,
                e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }

}
