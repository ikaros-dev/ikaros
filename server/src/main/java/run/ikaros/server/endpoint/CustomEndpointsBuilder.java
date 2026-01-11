package run.ikaros.server.endpoint;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.GroupVersionKind;
import run.ikaros.api.endpoint.CustomEndpoint;

public class CustomEndpointsBuilder
    implements EndpointsBuilder<CustomEndpoint, CustomEndpointsBuilder> {

    private final Map<GroupVersionKind, List<RouterFunction<ServerResponse>>> routerFunctionsMap;

    public CustomEndpointsBuilder() {
        routerFunctionsMap = new HashMap<>();
    }

    @Override
    public CustomEndpointsBuilder add(CustomEndpoint customEndpoint) {
        routerFunctionsMap
            .computeIfAbsent(customEndpoint.groupVersionKind(), gv -> new LinkedList<>())
            .add(customEndpoint.endpoint());
        return this;
    }

    @Override
    public RouterFunction<ServerResponse> build() {
        SpringdocRouteBuilder routeBuilder = SpringdocRouteBuilder.route();
        routerFunctionsMap.forEach((gvk, routerFunctions) -> {
            // routeBuilder.nest(RequestPredicates.path("/apis/"
            //         + gvk.group() + "/" + gvk.version() + "/" + gvk.kind()),
            //     () -> routerFunctions.stream().reduce(RouterFunction::and).orElse(null),
            //     builder -> builder.operationId("CustomEndpoints")
            //         .description("Custom Endpoint")
            //         .tag(gvk + "/CustomEndpoint")
            // );
            routeBuilder.nest(RequestPredicates.path("/apis/"
                    + gvk.group() + "/" + gvk.version() + "/" + gvk.kind()),
                () -> routerFunctions.stream().reduce(RouterFunction::and).orElse(null)
            );
        });
        if (routerFunctionsMap.isEmpty()) {
            // return empty route.
            return request -> Mono.empty();
        }
        routerFunctionsMap.clear();
        return routeBuilder.build();
    }
}
