package run.ikaros.server.custom.router;

import static run.ikaros.server.custom.CustomConverter.getNameFieldValue;
import static run.ikaros.server.custom.router.CustomRouterFunctionFactory.PathPatternGenerator.buildExtensionPathPattern;

import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.server.custom.Custom;
import run.ikaros.server.custom.CustomException;
import run.ikaros.server.custom.ReactiveCustomClient;

public class CustomCreateHandler implements CustomRouterFunctionFactory.GetHandler {
    private final ReactiveCustomClient customClient;
    private final Class<?> clazz;

    public CustomCreateHandler(ReactiveCustomClient customClient, Class<?> clazz) {
        this.customClient = customClient;
        this.clazz = clazz;
    }

    @Override
    public Mono<ServerResponse> handle(@NonNull ServerRequest request) {
        return request.bodyToMono(clazz)
            .switchIfEmpty(Mono.error(() -> new CustomException("Cannot read body to: " + clazz)))
            .flatMap(customClient::create)
            .flatMap(custom -> ServerResponse
                .created(URI.create(pathPattern()  + "/" + getNameFieldValue(custom)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(custom));
    }

    @Override
    public String pathPattern() {
        Custom custom = clazz.getAnnotation(Custom.class);
        return buildExtensionPathPattern(custom);
    }
}
