package run.ikaros.server.core.episode;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.apiresponse.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class EpisodeEndpoint implements CoreEndpoint {
    private final EpisodeService episodeService;

    public EpisodeEndpoint(EpisodeService episodeService) {
        this.episodeService = episodeService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/episode";
        return SpringdocRouteBuilder.route()
            .POST("/episode", this::postEpisode,
                builder -> builder.operationId("PostEpisode")
                    .tag(tag).description("Post episode.")
                    .requestBody(requestBodyBuilder()
                        .description("Episode")
                        .implementation(Episode.class))
                    .response(Builder.responseBuilder()
                        .implementation(Episode.class))
                )

            .PUT("/episode", this::putEpisode,
                builder -> builder.operationId("PutEpisode")
                    .tag(tag).description("Put episode.")
                    .requestBody(requestBodyBuilder()
                        .description("Episode")
                        .implementation(Episode.class))
                    .response(Builder.responseBuilder()
                        .implementation(Episode.class))
                )

            .DELETE("/episode/id/{id}", this::deleteById,
                builder -> builder.operationId("DeleteById")
                    .tag(tag).description("Delete episode by id.")
                    .parameter(parameterBuilder()
                        .name("id").required(true)
                        .in(ParameterIn.PATH)
                        .description("Episode id.")
                        .implementation(Long.class))
                    .response(Builder.responseBuilder()
                        .implementation(Episode.class))
            )

            .GET("/episode/{id}", this::getById,
                builder -> builder.operationId("GetById")
                    .tag(tag).description("Get episode by episode id.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .description("Episode id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(Builder.responseBuilder().implementation(Episode.class)))

            .GET("/episode/subjectId/{id}", this::getBySubjectIdAndGroupAndSequence,
                builder -> builder.operationId("GetById")
                    .tag(tag).description("Get episode by episode id.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .description("Subject id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("group")
                        .description("episode group")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(EpisodeGroup.class))
                    .parameter(parameterBuilder()
                        .name("sequence")
                        .description("episode sequence")
                        .required(true)
                        .in(ParameterIn.QUERY)
                        .implementation(EpisodeGroup.class))
                    .response(Builder.responseBuilder().implementation(Episode.class)))

            .GET("/episodes/subjectId/{id}", this::getAllBySubjectId,
                builder -> builder.operationId("getAllBySubjectId")
                    .tag(tag).description("Get all by subject id.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .description("Subject id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(Builder.responseBuilder().implementationArray(Episode.class))
            )

            .GET("/episode/attachment/refs/{id}", this::getAttachmentRefsById,
                builder -> builder.operationId("GetAttachmentRefsById")
                    .tag(tag).description("Get attachment refs by episode id.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .description("Episode id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(Builder.responseBuilder()
                        .description("Episode resource list.")
                        .implementationArray(EpisodeResource.class)))

            .GET("/episode/count/total/subjectId/{id}", this::getCountTotalBySubjectId,
                builder -> builder.operationId("GetCountTotalBySubjectId")
                    .tag(tag).description("Get count total by subject id.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .description("Subject id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(Builder.responseBuilder()
                        .description("Episode count for subject id.")
                        .implementation(Long.class)
                    )
            )

            .GET("/episode/count/matching/subjectId/{id}", this::getCountMatchingBySubjectId,
                builder -> builder.operationId("GetCountMatchingBySubjectId")
                    .tag(tag).description("Get count matching by subject id.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .description("Subject id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(Builder.responseBuilder()
                        .description("Episode count for subject id.")
                        .implementation(Long.class)
                    )
            )

            .build();
    }

    private Mono<ServerResponse> getCountTotalBySubjectId(ServerRequest request) {
        String id = request.pathVariable("id");
        Long subjectId = Long.valueOf(id);
        return episodeService.countBySubjectId(subjectId)
            .flatMap(count -> ServerResponse.ok().bodyValue(count));
    }

    private Mono<ServerResponse> getCountMatchingBySubjectId(ServerRequest request) {
        String id = request.pathVariable("id");
        Long subjectId = Long.valueOf(id);
        return episodeService.countMatchingBySubjectId(subjectId)
            .flatMap(count -> ServerResponse.ok().bodyValue(count));
    }

    private Mono<ServerResponse> deleteById(ServerRequest request) {
        String id = request.pathVariable("id");
        Long episodeId = Long.valueOf(id);
        return episodeService.deleteById(episodeId)
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> getById(ServerRequest request) {
        String id = request.pathVariable("id");
        Long episodeId = Long.valueOf(id);
        return episodeService.findById(episodeId)
            .flatMap(episode -> ServerResponse.ok().bodyValue(episode));
    }

    private Mono<ServerResponse> getBySubjectIdAndGroupAndSequence(ServerRequest request) {
        String id = request.pathVariable("id");
        Long subjectId = StringUtils.isNotBlank(id) ? Long.parseLong(id) : -1L;
        EpisodeGroup group =
            EpisodeGroup.valueOf(request.queryParam("group").orElse(EpisodeGroup.MAIN.name()));
        Float sequence = Float.valueOf(request.queryParam("sequence").orElse("0"));
        return episodeService.findBySubjectIdAndGroupAndSequence(subjectId, group, sequence)
            .flatMap(episode -> ServerResponse.ok().bodyValue(episode));
    }

    private Mono<ServerResponse> postEpisode(ServerRequest request) {
        return request.bodyToMono(Episode.class)
            .flatMap(episodeService::save)
            .flatMap(episode -> ServerResponse.ok().bodyValue(episode));
    }

    private Mono<ServerResponse> putEpisode(ServerRequest request) {
        return request.bodyToMono(Episode.class)
            .flatMap(episodeService::save)
            .flatMap(episode -> ServerResponse.ok().bodyValue(episode));
    }

    private Mono<ServerResponse> getAllBySubjectId(ServerRequest request) {
        String id = request.pathVariable("id");
        Long subjectId = Long.valueOf(id);
        return episodeService.findAllBySubjectId(subjectId)
            .collectList()
            .flatMap(episodes -> ServerResponse.ok().bodyValue(episodes));
    }

    private Mono<ServerResponse> getAttachmentRefsById(ServerRequest request) {
        String id = request.pathVariable("id");
        Long episodeId = Long.valueOf(id);
        return episodeService.findResourcesById(episodeId)
            .collectList()
            .flatMap(episodeResources -> ServerResponse.ok().bodyValue(episodeResources));
    }

}
