package run.ikaros.server.custom.router;

import static run.ikaros.server.custom.router.CustomRouterFunctionFactory.PathPatternGenerator.buildCustomPathPatternPrefix;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.custom.scheme.CustomScheme;
import run.ikaros.server.custom.event.CustomUpdateEvent;

@Slf4j
public class CustomUpdateMetaHandler implements CustomRouterFunctionFactory.UpdateMetaHandler {
    private final ReactiveCustomClient customClient;
    private final CustomScheme scheme;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Construct.
     */
    public CustomUpdateMetaHandler(ReactiveCustomClient customClient, CustomScheme scheme,
                                   ApplicationEventPublisher applicationEventPublisher) {
        this.customClient = customClient;
        this.scheme = scheme;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Mono<ServerResponse> handle(ServerRequest request) {
        var customName = request.pathVariable("name");
        var metaName = request.pathVariable("metaName");
        return request.bodyToMono(byte[].class)
            .flatMap(bytes -> customClient.updateOneMeta(scheme.type(), customName, metaName, bytes)
                .doOnSuccess(unused -> applicationEventPublisher.publishEvent(
                    new CustomUpdateEvent(this, scheme, customName)))
                .then(ServerResponse.ok().build()));
    }

    @Override
    public String pathPattern() {
        return buildCustomPathPatternPrefix(scheme)
            + '/' + scheme.singular()
            + "/{name}"
            + "/{metaName}";
    }

    private Mono<Object> transformMetaBytes2Instance(String metaName, byte[] bytes) {
        Object instance = null;
        try {
            instance = scheme.type().getDeclaredField(metaName).get(bytes);
        } catch (ReflectiveOperationException e) {
            log.warn("Transform metadata field bytes to instance fail "
                + "for class [{}] and field [{}].", scheme.type(), metaName, e);
        }
        return Mono.justOrEmpty(Objects.isNull(instance) ? bytes : instance);
    }
}
