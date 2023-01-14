package run.ikaros.server.custom.router;

import static run.ikaros.server.custom.router.CustomRouterFunctionFactory.PathPatternGenerator.buildExtensionPathPattern;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import run.ikaros.server.custom.Custom;
import run.ikaros.server.custom.ReactiveCustomClient;

public class CustomUpdateHandler implements CustomRouterFunctionFactory.GetHandler {
    private final ReactiveCustomClient customClient;
    private final Class<?> clazz;

    public CustomUpdateHandler(ReactiveCustomClient customClient, Class<?> clazz) {
        this.customClient = customClient;
        this.clazz = clazz;
    }

    @Override
    public Mono<ServerResponse> handle(@NonNull ServerRequest request) {
        var customName = request.pathVariable("name");
        return request.bodyToMono(clazz)
            .switchIfEmpty(Mono.error(() -> new ServerWebInputException(
                "Can not read body to:" + clazz
            )))
            .flatMap(customClient::update)
            .flatMap(updated -> ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updated));
    }

    @Override
    public String pathPattern() {
        Custom custom = clazz.getAnnotation(Custom.class);
        return buildExtensionPathPattern(custom) + "/{name}";
    }
}
