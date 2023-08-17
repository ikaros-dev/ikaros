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
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class EpisodeCollectionEndpoint implements CoreEndpoint {
    private final EpisodeCollectionService episodeCollectionService;

    public EpisodeCollectionEndpoint(EpisodeCollectionService episodeCollectionService) {
        this.episodeCollectionService = episodeCollectionService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/Collection/Episode";
        return SpringdocRouteBuilder.route()
            .GET("/collection/episode/{userId}/{episodeId}", this::findEpisodeCollection,
                builder -> builder.operationId("FindEpisodeCollection")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("userId")
                        .description("User id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("episodeId")
                        .description("Episode id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder()
                        .implementation(EpisodeCollection.class)))

            .PUT("/collection/episode/{userId}/{episodeId}",
                this::updateEpisodeCollection,
                builder -> builder.operationId("UpdateEpisodeCollection")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("userId")
                        .description("User id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("episodeId")
                        .description("Episode id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("progress")
                        .description("Episode collection progress.")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("duration")
                        .description("Episode collection duration.")
                        .in(ParameterIn.QUERY)
                        .required(false)
                        .implementation(Long.class))
            )

            .PUT("/collection/episode/finish/{userId}/{episodeId}/{finish}",
                this::updateEpisodeCollectionFinish,
                builder -> builder.operationId("UpdateEpisodeCollectionFinish")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("userId")
                        .description("User id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
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

            .POST("/collection/episode/{userId}/{episodeId}", this::create,
                builder -> builder.operationId("SaveEpisodeCollection")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("userId")
                        .description("User id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("episodeId")
                        .description("Episode id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder()
                        .implementation(EpisodeCollection.class))
            )

            .DELETE("/collection/episode/{userId}/{episodeId}", this::remove,
                builder -> builder.operationId("DeleteEpisodeCollection")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("userId")
                        .description("User id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
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
        String userId = serverRequest.pathVariable("userId");
        String episodeId = serverRequest.pathVariable("episodeId");
        return episodeCollectionService
            .findByUserIdAndEpisodeId(Long.valueOf(userId), Long.valueOf(episodeId))
            .flatMap(episodeCollection -> ServerResponse.ok()
                .bodyValue(episodeCollection))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> updateEpisodeCollection(ServerRequest serverRequest) {
        String userId = serverRequest.pathVariable("userId");
        String episodeId = serverRequest.pathVariable("episodeId");
        Optional<String> progressOp = serverRequest.queryParam("progress");
        Long progress = progressOp.map(Long::valueOf).orElse(0L);
        Optional<String> durationOp = serverRequest.queryParam("duration");
        Long duration = durationOp.map(Long::valueOf).orElse(null);
        return episodeCollectionService
            .updateEpisodeCollection(
                Long.valueOf(userId),
                Long.valueOf(episodeId),
                progress, duration)
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> updateEpisodeCollectionFinish(ServerRequest serverRequest) {
        String userId = serverRequest.pathVariable("userId");
        String episodeId = serverRequest.pathVariable("episodeId");
        String finish = serverRequest.pathVariable("finish");
        return episodeCollectionService
            .updateEpisodeCollectionFinish(
                Long.valueOf(userId),
                Long.valueOf(episodeId),
                Boolean.valueOf(finish))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> create(ServerRequest serverRequest) {
        String userId = serverRequest.pathVariable("userId");
        String episodeId = serverRequest.pathVariable("episodeId");
        return episodeCollectionService.create(Long.valueOf(userId), Long.valueOf(episodeId))
            .flatMap(episodeCollection -> ServerResponse.ok().bodyValue(episodeCollection));
    }

    private Mono<ServerResponse> remove(ServerRequest serverRequest) {
        String userId = serverRequest.pathVariable("userId");
        String episodeId = serverRequest.pathVariable("episodeId");
        return episodeCollectionService.remove(Long.valueOf(userId), Long.valueOf(episodeId))
            .flatMap(episodeCollection -> ServerResponse.ok().bodyValue(episodeCollection));
    }


}
