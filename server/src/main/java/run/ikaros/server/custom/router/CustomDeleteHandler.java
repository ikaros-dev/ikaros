package run.ikaros.server.custom.router;

import static run.ikaros.server.custom.router.CustomRouterFunctionFactory.PathPatternGenerator.buildCustomPathPatternPrefix;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.custom.scheme.CustomScheme;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.server.custom.CustomConverter;
import run.ikaros.server.custom.event.CustomDeleteEvent;

public class CustomDeleteHandler implements CustomRouterFunctionFactory.DeleteHandler {
    private final ReactiveCustomClient customClient;
    private final CustomScheme scheme;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Construct.
     */
    public CustomDeleteHandler(ReactiveCustomClient customClient,
                               CustomScheme scheme,
                               ApplicationEventPublisher applicationEventPublisher) {
        this.customClient = customClient;
        this.scheme = scheme;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Mono<ServerResponse> handle(@NonNull ServerRequest request) {
        var customName = request.pathVariable("name");
        return customClient.delete(scheme.type(), customName)
            .doOnSuccess(custom -> applicationEventPublisher.publishEvent(
                new CustomDeleteEvent(this, scheme, CustomConverter.getNameFieldValue(custom))))
            .flatMap(custom -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(custom))
            .onErrorResume(NotFoundException.class,
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
