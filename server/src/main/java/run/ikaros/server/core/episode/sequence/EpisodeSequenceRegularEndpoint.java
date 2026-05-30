package run.ikaros.server.core.episode.sequence;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.episode.EpisodeSequenceRegular;
import run.ikaros.api.core.episode.EpisodeSequenceRegularResult;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class EpisodeSequenceRegularEndpoint implements CoreEndpoint {

    private final EpisodeSequenceRegularService service;

    public EpisodeSequenceRegularEndpoint(EpisodeSequenceRegularService service) {
        this.service = service;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/episode";
        return SpringdocRouteBuilder.route()

            .POST("/episode/sequence-regular", this::createRegular,
                builder -> builder.operationId("CreateEpisodeSequenceRegular")
                    .tag(tag)
                    .description("Create a new episode sequence regular rule.")
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .implementation(EpisodeSequenceRegular.class))
                    .response(responseBuilder()
                        .implementation(EpisodeSequenceRegular.class))
            )

            .PUT("/episode/sequence-regular", this::updateRegular,
                builder -> builder.operationId("UpdateEpisodeSequenceRegular")
                    .tag(tag)
                    .description("Update an existing episode sequence regular rule.")
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .implementation(EpisodeSequenceRegular.class))
                    .response(responseBuilder()
                        .implementation(EpisodeSequenceRegular.class))
            )

            .DELETE("/episode/sequence-regular/{id}", this::deleteRegular,
                builder -> builder.operationId("DeleteEpisodeSequenceRegular")
                    .tag(tag)
                    .description("Delete an episode sequence regular rule by ID.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .description("Rule ID.")
                        .required(true)
                        .implementation(UUID.class))
            )

            .GET("/episode/sequence-regular/{id}", this::getRegular,
                builder -> builder.operationId("GetEpisodeSequenceRegular")
                    .tag(tag)
                    .description("Get a single rule by ID.")
                    .parameter(parameterBuilder()
                        .name("id")
                        .description("Rule ID.")
                        .required(true)
                        .implementation(UUID.class))
                    .response(responseBuilder()
                        .implementation(EpisodeSequenceRegular.class))
            )

            .GET("/episode/sequence-regulars", this::listRegulars,
                builder -> builder.operationId("ListEpisodeSequenceRegulars")
                    .tag(tag)
                    .description("List rules with pagination.")
                    .parameter(parameterBuilder()
                        .name("page")
                        .description("Page number, starts from 1. Default 1.")
                        .required(false)
                        .implementation(Integer.class))
                    .parameter(parameterBuilder()
                        .name("size")
                        .description("Page size. Default 10.")
                        .required(false)
                        .implementation(Integer.class))
                    .response(responseBuilder()
                        .implementation(PagingWrap.class))
            )

            .GET("/episode/sequence-regular/match", this::matchAttachment,
                builder -> builder.operationId("MatchEpisodeSequenceRegular")
                    .tag(tag)
                    .description("Match an attachment name against all enabled rules.")
                    .parameter(parameterBuilder()
                        .name("attachmentName")
                        .description("The attachment name to match.")
                        .required(true)
                        .implementation(String.class))
                    .response(responseBuilder()
                        .implementation(EpisodeSequenceRegularResult.class))
            )

            .build();
    }

    private Mono<ServerResponse> createRegular(ServerRequest request) {
        return request.bodyToMono(EpisodeSequenceRegular.class)
            .flatMap(service::save)
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> updateRegular(ServerRequest request) {
        return request.bodyToMono(EpisodeSequenceRegular.class)
            .flatMap(service::save)
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> deleteRegular(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return service.removeById(id)
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> getRegular(ServerRequest request) {
        UUID id = UUID.fromString(request.pathVariable("id"));
        return service.findById(id)
            .flatMap(result -> ServerResponse.ok().bodyValue(result))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> listRegulars(ServerRequest request) {
        Integer page = request.queryParam("page")
            .map(Integer::valueOf).orElse(null);
        Integer size = request.queryParam("size")
            .map(Integer::valueOf).orElse(null);
        return service.findAll(page, size)
            .flatMap(paging -> ServerResponse.ok().bodyValue(paging));
    }

    private Mono<ServerResponse> matchAttachment(ServerRequest request) {
        String attachmentName = request.queryParam("attachmentName").orElse("");
        return service.match(attachmentName)
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }
}
