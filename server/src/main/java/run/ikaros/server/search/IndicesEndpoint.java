package run.ikaros.server.search;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class IndicesEndpoint implements CoreEndpoint {
    private final IndicesService indicesService;

    public IndicesEndpoint(IndicesService indicesService) {
        this.indicesService = indicesService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = OpenApiConst.CORE_VERSION + "/Indices";
        return SpringdocRouteBuilder.route()
            .POST("indices/file", this::rebuildFileIndices,
                builder -> builder.operationId("BuildFileIndices")
                    .tag(tag)
                    .description("Build or rebuild file indices for full text search"))
            .POST("indices/subject", this::rebuildSubjectIndices,
                builder -> builder.operationId("BuildSubjectIndices")
                    .tag(tag)
                    .description("Build or rebuild subject indices for full text search"))
            .build();
    }

    private Mono<ServerResponse> rebuildFileIndices(ServerRequest request) {
        return indicesService.rebuildFileIndices()
            .then(Mono.defer(() -> ServerResponse.ok().bodyValue("Rebuild file indices")));
    }

    private Mono<ServerResponse> rebuildSubjectIndices(ServerRequest request) {
        return indicesService.rebuildSubjectIndices()
            .then(Mono.defer(() -> ServerResponse.ok().bodyValue("Rebuild subject indices")));
    }
}
