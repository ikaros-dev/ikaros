package run.ikaros.server.custom.router;

import static run.ikaros.server.custom.router.CustomRouterFunctionFactory.PathPatternGenerator.buildExtensionPathPattern;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.server.custom.Custom;
import run.ikaros.server.custom.ReactiveCustomClient;

public class CustomGetHandler implements CustomRouterFunctionFactory.GetHandler {
    private final ReactiveCustomClient customClient;
    private final Class<?> clazz;

    public CustomGetHandler(ReactiveCustomClient customClient, Class<?> clazz) {
        this.customClient = customClient;
        this.clazz = clazz;
    }

    @Override
    public Mono<ServerResponse> handle(@NonNull ServerRequest request) {
        var customName = request.pathVariable("name");
        return customClient.findOne(clazz, customName)
            .flatMap(custom -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(custom));
    }

    @Override
    public String pathPattern() {
        Custom custom = clazz.getAnnotation(Custom.class);
        return buildExtensionPathPattern(custom) + "/{name}";
    }
}
