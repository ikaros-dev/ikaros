package run.ikaros.server.core.subject.endpoint;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.server.core.subject.service.EpisodeFileService;
import run.ikaros.server.core.subject.vo.BatchMatchingEpisodeFile;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class EpisodeFileEndpoint implements CoreEndpoint {
    private final EpisodeFileService episodeFileService;

    public EpisodeFileEndpoint(EpisodeFileService episodeFileService) {
        this.episodeFileService = episodeFileService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/EpisodeFile";
        return SpringdocRouteBuilder.route()
            .POST("/episodefile/{episodeId}/{fileId}", this::create,
                builder -> builder.operationId("CreateEpisodeFile")
                    .tag(tag).description("Create episode and file bind record.")
                    .parameter(parameterBuilder()
                        .name("episodeId")
                        .description("Episode id.")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("fileId")
                        .description("File id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
            )
            .DELETE("/episodefile/{episodeId}/{fileId}", this::remove,
                builder -> builder.operationId("RemoveEpisodeFile")
                    .tag(tag).description("Remove episode and file bind record.")
                    .parameter(parameterBuilder()
                        .name("episodeId")
                        .description("Episode id.")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("fileId")
                        .description("File id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
            )
            .POST("/episodefile/batch", this::batchMatchingEpisodeFile,
                builder -> builder.operationId("BatchMatchingEpisodeFile")
                    .tag(tag).description("Batch matching episode file.")
                    .requestBody(Builder.requestBodyBuilder()
                        .required(true)
                        .description("batch matching episode file request value object.")
                        .implementation(BatchMatchingEpisodeFile.class))
            )
            .build();
    }

    private Mono<ServerResponse> batchMatchingEpisodeFile(ServerRequest request) {
        return request.bodyToMono(BatchMatchingEpisodeFile.class)
            .flatMap(batchMatchingEpisodeFile -> episodeFileService.batchMatching(
                batchMatchingEpisodeFile.getSubjectId(), batchMatchingEpisodeFile.getFileIds()))
            .then(ServerResponse.ok().build());
    }

    private Long assertAndGetPathLongVar(ServerRequest request, String pathVarName) {
        String pathVarStr = request.pathVariable(pathVarName);
        Assert.hasText(pathVarStr, "'" + pathVarName + "' must has text.");
        return Long.valueOf(pathVarStr);
    }

    private Mono<ServerResponse> create(ServerRequest request) {
        Long episodeId = assertAndGetPathLongVar(request, "episodeId");
        Long fileId = assertAndGetPathLongVar(request, "fileId");
        return episodeFileService.create(episodeId, fileId)
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> remove(ServerRequest request) {
        Long episodeId = assertAndGetPathLongVar(request, "episodeId");
        Long fileId = assertAndGetPathLongVar(request, "fileId");
        return episodeFileService.remove(episodeId, fileId)
            .then(ServerResponse.ok().build());
    }
}
