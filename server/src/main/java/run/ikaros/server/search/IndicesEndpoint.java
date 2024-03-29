package run.ikaros.server.search;

import static run.ikaros.server.infra.utils.GenericClassUtils.generateConcreteClass;
import static run.ikaros.server.infra.utils.QueryParamBuildUtil.buildParametersFromType;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.apiresponse.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.search.SearchParam;
import run.ikaros.api.search.SearchResult;
import run.ikaros.api.search.subject.SubjectHint;
import run.ikaros.api.search.subject.SubjectSearchService;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class IndicesEndpoint implements CoreEndpoint {
    private final IndicesService indicesService;
    private final SubjectSearchService subjectSearchService;

    /**
     * Construct.
     */
    public IndicesEndpoint(IndicesService indicesService,
                           SubjectSearchService subjectSearchService) {
        this.indicesService = indicesService;
        this.subjectSearchService = subjectSearchService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = OpenApiConst.CORE_VERSION + "/indices";
        return SpringdocRouteBuilder.route()

            .POST("indices/subject", this::rebuildSubjectIndices,
                builder -> builder.operationId("BuildSubjectIndices")
                    .tag(tag)
                    .description("Build or rebuild subject indices for full text search"))

            .GET("indices/subject", this::searchSubject,
                builder -> {
                    builder.operationId("SearchSubject")
                        .tag(tag)
                        .description("Search subjects with fuzzy query")
                        .response(Builder.responseBuilder().implementation(
                            generateConcreteClass(SearchResult.class, SubjectHint.class,
                                () -> "SubjectHints")));
                    buildParametersFromType(builder, SearchParam.class);
                }
            )

            .build();
    }


    private Mono<ServerResponse> rebuildSubjectIndices(ServerRequest request) {
        return indicesService.rebuildSubjectIndices()
            .then(Mono.defer(() -> ServerResponse.ok().bodyValue("Rebuild subject indices")));
    }

    private Mono<ServerResponse> searchSubject(ServerRequest request) {
        return Mono.fromSupplier(
                () -> new SearchParam(request.queryParams()))
            .map(searchParam -> {
                try {
                    return subjectSearchService.search(searchParam);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }
}
