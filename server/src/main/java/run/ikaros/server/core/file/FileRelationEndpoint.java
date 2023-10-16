package run.ikaros.server.core.file;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.file.FileRelations;
import run.ikaros.api.core.subject.Subtitle;
import run.ikaros.api.store.enums.FileRelationType;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class FileRelationEndpoint implements CoreEndpoint {

    private final FileRelationService fileRelationService;

    public FileRelationEndpoint(FileRelationService fileRelationService) {
        this.fileRelationService = fileRelationService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/file/relation";
        return SpringdocRouteBuilder.route()
            .GET("/file/relations", this::findFileRelations,
                builder -> builder
                    .operationId("FindFileRelations")
                    .tag(tag)
                    .parameter(parameterBuilder().name("fileId")
                        .description("File ID")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder().name("relationType")
                        .description("Relation type")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .implementation(FileRelationType.class))
                    .response(responseBuilder().implementation(FileRelations.class)))

            .GET("/file/relation/video/subtitle/{fileId}", this::findVideoSubtitles,
                builder -> builder
                    .operationId("FindVideoSubtitles")
                    .tag(tag)
                    .parameter(parameterBuilder().name("fileId")
                        .description("Video File ID")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder().implementationArray(Subtitle.class)))

            .build();
    }

    Mono<ServerResponse> findFileRelations(ServerRequest request) {
        String fileId = request.queryParam("fileId").orElse("");
        String relationType = request.queryParam("relationType").orElse("");
        if (!StringUtils.hasText(fileId) || !StringUtils.hasText(relationType)) {
            return ServerResponse.badRequest().bodyValue("request params incorrect.");
        }
        long fileIdL;
        FileRelationType relationTypeE;
        try {
            fileIdL = Long.parseLong(fileId);
            relationTypeE = FileRelationType.valueOf(relationType);
        } catch (IllegalArgumentException illegalArgumentException) {
            return ServerResponse.badRequest().bodyValue("request params incorrect.");
        }
        return fileRelationService.findFileRelations(relationTypeE, fileIdL)
            .flatMap(fileRelations -> ServerResponse.ok().bodyValue(fileRelations))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    Mono<ServerResponse> findVideoSubtitles(ServerRequest request) {
        String fileId = request.pathVariable("fileId");
        Long fileIdL = Long.valueOf(fileId);
        return fileRelationService.findVideoSubtitles(fileIdL)
            .collectList()
            .flatMap(subtitles -> ServerResponse.ok().bodyValue(subtitles))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

}
