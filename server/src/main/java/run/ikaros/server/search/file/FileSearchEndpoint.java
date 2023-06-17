package run.ikaros.server.search.file;

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
import run.ikaros.api.search.file.FileHint;
import run.ikaros.api.search.file.FileSearchService;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class FileSearchEndpoint implements CoreEndpoint {
    private final FileSearchService fileSearchService;

    public FileSearchEndpoint(FileSearchService fileSearchService) {
        this.fileSearchService = fileSearchService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        final var tag = OpenApiConst.CORE_VERSION + "/Indices";
        return SpringdocRouteBuilder.route()
            .GET("indices/file", this::search,
                builder -> {
                    builder.operationId("SearchFile")
                        .tag(tag)
                        .description("Search files with fuzzy query")
                        .response(Builder.responseBuilder().implementation(
                            generateConcreteClass(SearchResult.class, FileHint.class,
                                () -> "FileHints")));
                    buildParametersFromType(builder, SearchParam.class);
                }
            )
            .build();
    }

    private Mono<ServerResponse> search(ServerRequest request) {
        return Mono.fromSupplier(
                () -> new SearchParam(request.queryParams()))
            .map(searchParam -> {
                try {
                    return fileSearchService.search(searchParam);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }
}
