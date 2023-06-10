package run.ikaros.server.custom.router;

import static run.ikaros.server.custom.router.CustomRouterFunctionFactory.PathPatternGenerator.buildCustomPathPatternPrefix;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.custom.scheme.CustomScheme;
import run.ikaros.api.exception.NotFoundException;

public class CustomListHandler implements CustomRouterFunctionFactory.ListHandler {
    private final ReactiveCustomClient customClient;
    private final CustomScheme scheme;

    public CustomListHandler(ReactiveCustomClient customClient, CustomScheme scheme) {
        this.customClient = customClient;
        this.scheme = scheme;
    }

    @Override
    public Mono<ServerResponse> handle(@NonNull ServerRequest request) {
        return customClient.findAll(scheme.type(), null)
            .collectList()
            .flatMap(customList -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(customList))
            .onErrorResume(NotFoundException.class, e -> ServerResponse.notFound().build());
    }

    @Override
    public String pathPattern() {
        return buildCustomPathPatternPrefix(scheme)
            + '/' + scheme.plural();
    }
}
