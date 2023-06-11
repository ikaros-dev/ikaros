package run.ikaros.server.custom.router;

import static run.ikaros.server.custom.router.CustomRouterFunctionFactory.PathPatternGenerator.buildCustomPathPatternPrefix;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.custom.scheme.CustomScheme;
import run.ikaros.api.exception.NotFoundException;


public class CustomUpdateHandler implements CustomRouterFunctionFactory.UpdateHandler {
    private final ReactiveCustomClient customClient;
    private final CustomScheme scheme;

    public CustomUpdateHandler(ReactiveCustomClient customClient,
                               CustomScheme scheme) {
        this.customClient = customClient;
        this.scheme = scheme;
    }

    @Override
    public Mono<ServerResponse> handle(@NonNull ServerRequest request) {
        return request.bodyToMono(scheme.type())
            .switchIfEmpty(Mono.error(() -> new ServerWebInputException(
                "Can not read body to:" + scheme.type())))
            .flatMap(customClient::update)
            .flatMap(updated -> ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updated))
            .onErrorResume(NotFoundException.class, e -> ServerResponse.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(e.getMessage()));
    }

    @Override
    public String pathPattern() {
        return buildCustomPathPatternPrefix(scheme)
            + '/' + scheme.singular();
    }
}
