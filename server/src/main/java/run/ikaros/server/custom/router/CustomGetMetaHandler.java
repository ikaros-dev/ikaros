package run.ikaros.server.custom.router;

import static run.ikaros.server.custom.router.CustomRouterFunctionFactory.PathPatternGenerator.buildCustomPathPatternPrefix;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.custom.scheme.CustomScheme;
import run.ikaros.api.exception.NotFoundException;

@Slf4j
public class CustomGetMetaHandler implements CustomRouterFunctionFactory.GetMetaHandler {
    private final ReactiveCustomClient customClient;
    private final CustomScheme scheme;

    public CustomGetMetaHandler(ReactiveCustomClient customClient, CustomScheme scheme) {
        this.customClient = customClient;
        this.scheme = scheme;
    }

    @Override
    public Mono<ServerResponse> handle(ServerRequest request) {
        var customName = request.pathVariable("name");
        var metaName = request.pathVariable("metaName");
        return customClient.fetchOneMeta(scheme.type(), customName, metaName)
            .flatMap(metaVal -> ServerResponse.ok()
                .bodyValue(metaVal))
            .onErrorResume(NotFoundException.class, e -> ServerResponse.notFound().build());
    }

    @Override
    public String pathPattern() {
        return buildCustomPathPatternPrefix(scheme)
            + '/' + scheme.singular()
            + "/{name}"
            + "/{metaName}";
    }
}
