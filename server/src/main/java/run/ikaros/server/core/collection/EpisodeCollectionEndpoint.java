package run.ikaros.server.core.collection;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.collection.EpisodeCollection;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class EpisodeCollectionEndpoint implements CoreEndpoint {
    private final EpisodeCollectionService episodeCollectionService;
    private final UserService userService;

    public EpisodeCollectionEndpoint(EpisodeCollectionService episodeCollectionService,
                                     UserService userService) {
        this.episodeCollectionService = episodeCollectionService;
        this.userService = userService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/collection/episode";
        return SpringdocRouteBuilder.route()
            .GET("/collection/episode/{episodeId}", this::findEpisodeCollection,
                builder -> builder.operationId("FindEpisodeCollection")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("episodeId")
                        .description("Episode id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder()
                        .implementation(EpisodeCollection.class)))

            .GET("/collections/episode/subjectId/{subjectId}",
                this::findEpisodeCollectionsByUserIdAndSubjectId,
                builder -> builder.operationId("FindEpisodeCollectionsByUserIdAndSubjectId")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .description("Subject id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder()
                        .implementationArray(EpisodeCollection.class)))

            .PUT("/collection/episode/{episodeId}",
                this::updateEpisodeCollection,
                builder -> builder.operationId("UpdateEpisodeCollection")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("episodeId")
                        .description("Episode id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("progress")
                        .description("Episode collection progress, unit is milliseconds.")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("duration")
                        .description("Episode collection duration, unit is milliseconds.")
                        .in(ParameterIn.QUERY)
                        .required(false)
                        .implementation(Long.class))
            )

            .PUT("/collection/episode/finish/{episodeId}/{finish}",
                this::updateEpisodeCollectionFinish,
                builder -> builder.operationId("UpdateEpisodeCollectionFinish")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("episodeId")
                        .description("Episode id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("finish")
                        .description("Episode collection finish.")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Boolean.class)))

            .POST("/collection/episode/{episodeId}", this::create,
                builder -> builder.operationId("SaveEpisodeCollection")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("episodeId")
                        .description("Episode id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder()
                        .implementation(EpisodeCollection.class))
            )

            .DELETE("/collection/episode/{episodeId}", this::remove,
                builder -> builder.operationId("DeleteEpisodeCollection")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("episodeId")
                        .description("Episode id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder()
                        .implementation(EpisodeCollection.class))
            )

            .build();
    }

    private Mono<ServerResponse> findEpisodeCollection(ServerRequest serverRequest) {
        String episodeId = serverRequest.pathVariable("episodeId");
        return userService.getUserIdFromSecurityContext()
            .flatMap(userId -> episodeCollectionService
                .findByUserIdAndEpisodeId(userId, Long.valueOf(episodeId)))
            .flatMap(episodeCollection -> ServerResponse.ok()
                .bodyValue(episodeCollection))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> findEpisodeCollectionsByUserIdAndSubjectId(
        ServerRequest serverRequest) {
        String subjectId = serverRequest.pathVariable("subjectId");
        return userService.getUserIdFromSecurityContext()
            .flatMapMany(userId -> episodeCollectionService
                .findAllByUserIdAndSubjectId(userId, Long.valueOf(subjectId)))
            .collectList()
            .flatMap(episodeCollections -> ServerResponse.ok()
                .bodyValue(episodeCollections));
    }

    private Mono<ServerResponse> updateEpisodeCollection(ServerRequest serverRequest) {
        String episodeId = serverRequest.pathVariable("episodeId");
        Optional<String> progressOp = serverRequest.queryParam("progress");
        Long progress = progressOp.map(Long::valueOf).orElse(0L);
        Optional<String> durationOp = serverRequest.queryParam("duration");
        Long duration = durationOp.map(Long::valueOf).orElse(null);
        return userService.getUserIdFromSecurityContext()
            .flatMap(userId -> episodeCollectionService
                .updateEpisodeCollection(
                    userId,
                    Long.valueOf(episodeId),
                    progress, duration))
            .then(ServerResponse.ok().build())
            .onErrorResume(IllegalArgumentException.class,
                e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }

    private Mono<ServerResponse> updateEpisodeCollectionFinish(ServerRequest serverRequest) {
        String episodeId = serverRequest.pathVariable("episodeId");
        String finish = serverRequest.pathVariable("finish");
        return userService.getUserIdFromSecurityContext()
            .flatMap(userId -> episodeCollectionService
                .updateEpisodeCollectionFinish(
                    userId,
                    Long.valueOf(episodeId),
                    Boolean.valueOf(finish)))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> create(ServerRequest serverRequest) {
        String episodeId = serverRequest.pathVariable("episodeId");
        return userService.getUserIdFromSecurityContext()
            .flatMap(userId -> episodeCollectionService.create(userId, Long.valueOf(episodeId)))
            .flatMap(episodeCollection -> ServerResponse.ok().bodyValue(episodeCollection));
    }

    private Mono<ServerResponse> remove(ServerRequest serverRequest) {
        String episodeId = serverRequest.pathVariable("episodeId");
        return userService.getUserIdFromSecurityContext()
            .flatMap(userId -> episodeCollectionService.remove(userId, Long.valueOf(episodeId)))
            .flatMap(episodeCollection -> ServerResponse.ok().bodyValue(episodeCollection));
    }


}
