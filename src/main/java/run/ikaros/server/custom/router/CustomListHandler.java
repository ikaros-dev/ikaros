package run.ikaros.server.custom.router;

import static run.ikaros.server.custom.router.CustomRouterFunctionFactory.PathPatternGenerator.buildExtensionPathPattern;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.server.custom.Custom;
import run.ikaros.server.custom.ReactiveCustomClient;

public class CustomListHandler implements CustomRouterFunctionFactory.GetHandler {
    private final ReactiveCustomClient customClient;
    private final Class<?> clazz;

    public CustomListHandler(ReactiveCustomClient customClient, Class<?> clazz) {
        this.customClient = customClient;
        this.clazz = clazz;
    }

    @Override
    public Mono<ServerResponse> handle(@NonNull ServerRequest request) {
        return customClient.findAll(clazz, null)
            .collectList()
            .flatMap(customList -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(customList));
    }

    @Override
    public String pathPattern() {
        Custom custom = clazz.getAnnotation(Custom.class);
        return buildExtensionPathPattern(custom);
    }
}
