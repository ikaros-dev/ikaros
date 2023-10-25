package run.ikaros.server.custom.router;

import static run.ikaros.server.custom.CustomConverter.getNameFieldValue;
import static run.ikaros.server.custom.router.CustomRouterFunctionFactory.PathPatternGenerator.buildCustomPathPatternPrefix;

import java.net.URI;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.custom.scheme.CustomScheme;
import run.ikaros.api.infra.exception.IkarosNotFoundException;
import run.ikaros.api.infra.exception.custom.CustomException;
import run.ikaros.server.custom.CustomConverter;
import run.ikaros.server.custom.event.CustomCreateEvent;

public class CustomCreateHandler implements CustomRouterFunctionFactory.CreateHandler {
    private final ReactiveCustomClient customClient;
    private final CustomScheme scheme;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Construct.
     */
    public CustomCreateHandler(ReactiveCustomClient customClient,
                               CustomScheme scheme,
                               ApplicationEventPublisher applicationEventPublisher) {
        this.customClient = customClient;
        this.scheme = scheme;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Mono<ServerResponse> handle(@NonNull ServerRequest request) {
        return request.bodyToMono(scheme.type())
            .switchIfEmpty(
                Mono.error(() -> new CustomException("Cannot read body to: " + scheme.type())))
            .flatMap(customClient::create)
            .doOnSuccess(
                custom -> applicationEventPublisher.publishEvent(
                    new CustomCreateEvent(this, scheme, CustomConverter.getNameFieldValue(custom))))
            .flatMap(custom -> ServerResponse
                .created(URI.create(pathPattern() + "/" + getNameFieldValue(custom)))
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
            + '/' + scheme.singular();
    }
}
