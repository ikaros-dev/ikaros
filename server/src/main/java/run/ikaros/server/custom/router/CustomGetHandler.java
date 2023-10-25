package run.ikaros.server.custom.router;

import static run.ikaros.server.custom.router.CustomRouterFunctionFactory.PathPatternGenerator.buildCustomPathPatternPrefix;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.custom.scheme.CustomScheme;
import run.ikaros.api.infra.exception.IkarosNotFoundException;

public class CustomGetHandler implements CustomRouterFunctionFactory.GetHandler {
    private final ReactiveCustomClient customClient;
    private final CustomScheme scheme;

    public CustomGetHandler(ReactiveCustomClient customClient, CustomScheme scheme) {
        this.customClient = customClient;
        this.scheme = scheme;
    }

    @Override
    public Mono<ServerResponse> handle(@NonNull ServerRequest request) {
        var customName = request.pathVariable("name");
        return customClient.findOne(scheme.type(), customName)
            .flatMap(custom -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(custom))
            .onErrorResume(IkarosNotFoundException.class,
                e -> ServerResponse.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(e.getMessage()));
    }

    @Override
    public String pathPattern() {
        return buildCustomPathPatternPrefix(scheme)
            + '/' + scheme.singular()
            + "/{name}";
    }
}
