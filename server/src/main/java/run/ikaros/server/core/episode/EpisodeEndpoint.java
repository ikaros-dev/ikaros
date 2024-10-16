package run.ikaros.server.core.episode;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

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
            .GET("/episode/{id}", this::findById,
                builder -> builder.operationId("FindEpisodeById")
                    .tag(tag).description("Find episode by episode id.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .description("Episode id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(Builder.responseBuilder().implementation(Episode.class)))

            .GET("/episode/attachment/refs/{id}", this::findAttachmentRefsById,
                builder -> builder.operationId("FindEpisodeAttachmentRefsById")
                    .tag(tag).description("Find episode all attachment refs by episode id.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .description("Episode id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(Builder.responseBuilder()
                        .description("Episode resource list.")
                        .implementationArray(EpisodeResource.class)))

            .GET("/episode/count/total/subjectId/{id}", this::countTotalBySubjectId,
                builder -> builder.operationId("CountEpisodeById")
                    .tag(tag).description("Count episode by subject id.")
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

            .GET("/episode/count/matching/subjectId/{id}", this::countMatchingBySubjectId,
                builder -> builder.operationId("CountEpisodeById")
                    .tag(tag).description("Count episode by subject id.")
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

    private Mono<ServerResponse> countTotalBySubjectId(ServerRequest request) {
        String id = request.pathVariable("id");
        Long subjectId = Long.valueOf(id);
        return episodeService.countBySubjectId(subjectId)
            .flatMap(count -> ServerResponse.ok().bodyValue(count));
    }

    private Mono<ServerResponse> countMatchingBySubjectId(ServerRequest request) {
        String id = request.pathVariable("id");
        Long subjectId = Long.valueOf(id);
        return episodeService.countMatchingBySubjectId(subjectId)
            .flatMap(count -> ServerResponse.ok().bodyValue(count));
    }

    private Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("id");
        Long episodeId = Long.valueOf(id);
        return episodeService.findById(episodeId)
            .flatMap(episode -> ServerResponse.ok().bodyValue(episode));
    }

    private Mono<ServerResponse> findAttachmentRefsById(ServerRequest request) {
        String id = request.pathVariable("id");
        Long episodeId = Long.valueOf(id);
        return episodeService.findResourcesById(episodeId)
            .collectList()
            .flatMap(episodeResources -> ServerResponse.ok().bodyValue(episodeResources));
    }

}
