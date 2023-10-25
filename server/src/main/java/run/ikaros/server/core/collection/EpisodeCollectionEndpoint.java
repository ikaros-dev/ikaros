package run.ikaros.server.core.collection;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static run.ikaros.api.infra.model.ResponseResult.success;

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
import run.ikaros.api.infra.model.ResponseResult;
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
        var tag = OpenApiConst.CORE_VERSION + "/episode/collection";
        return SpringdocRouteBuilder.route()
            .GET("/episode/collection/{userId}/{episodeId}", this::findEpisodeCollection,
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
                        .implementation(ResponseResult.class)))

            .GET("/episode/collections/subjectId/{userId}/{subjectId}",
                this::findEpisodeCollectionsByUserIdAndSubjectId,
                builder -> builder.operationId("FindEpisodeCollectionsByUserIdAndSubjectId")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("userId")
                        .description("User id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("subjectId")
                        .description("Subject id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))

            .PUT("/episode/collection/{userId}/{episodeId}",
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
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))

            .PUT("/episode/collection/finish/{userId}/{episodeId}/{finish}",
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
                        .implementation(Boolean.class))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))

            .POST("/episode/collection/{userId}/{episodeId}", this::create,
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
                        .implementation(ResponseResult.class)))

            .DELETE("/episode/collection/{userId}/{episodeId}", this::remove,
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
                        .implementation(ResponseResult.class)))

            .build();
    }

    private Mono<ServerResponse> findEpisodeCollection(ServerRequest serverRequest) {
        String userId = serverRequest.pathVariable("userId");
        String episodeId = serverRequest.pathVariable("episodeId");
        return episodeCollectionService
            .findByUserIdAndEpisodeId(Long.valueOf(userId), Long.valueOf(episodeId))
            .flatMap(episodeCollection -> ok().bodyValue(success(episodeCollection)));
    }

    private Mono<ServerResponse> findEpisodeCollectionsByUserIdAndSubjectId(
        ServerRequest serverRequest) {
        String userId = serverRequest.pathVariable("userId");
        String subjectId = serverRequest.pathVariable("subjectId");
        return episodeCollectionService
            .findAllByUserIdAndSubjectId(Long.valueOf(userId), Long.valueOf(subjectId))
            .collectList()
            .flatMap(episodeCollections -> ok().bodyValue(success(episodeCollections)));
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
            .then(ok().bodyValue(success()));
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
            .then(ok().bodyValue(success()));
    }

    private Mono<ServerResponse> create(ServerRequest serverRequest) {
        String userId = serverRequest.pathVariable("userId");
        String episodeId = serverRequest.pathVariable("episodeId");
        return episodeCollectionService.create(Long.valueOf(userId), Long.valueOf(episodeId))
            .flatMap(episodeCollection -> ok().bodyValue(episodeCollection));
    }

    private Mono<ServerResponse> remove(ServerRequest serverRequest) {
        String userId = serverRequest.pathVariable("userId");
        String episodeId = serverRequest.pathVariable("episodeId");
        return episodeCollectionService.remove(Long.valueOf(userId), Long.valueOf(episodeId))
            .flatMap(episodeCollection -> ok().bodyValue(episodeCollection));
    }


}
