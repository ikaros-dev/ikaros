package run.ikaros.server.custom.router;

import static run.ikaros.server.custom.router.CustomRouterFunctionFactory.PathPatternGenerator.buildExtensionPathPattern;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.server.custom.ReactiveCustomClient;
import run.ikaros.server.custom.scheme.CustomScheme;

public class CustomListHandler implements CustomRouterFunctionFactory.GetHandler {
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
                .bodyValue(customList));
    }

    @Override
    public String pathPattern() {
        return buildExtensionPathPattern(scheme.groupVersionKind());
    }
}
