package run.ikaros.server.endpoint;

import java.util.ArrayList;
import java.util.List;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import run.ikaros.api.constant.OpenApiConst;

public class CoreEndpointsBuilder implements EndpointsBuilder<CoreEndpoint, CoreEndpointsBuilder> {
    private final List<RouterFunction<ServerResponse>> routerFunctions;

    public CoreEndpointsBuilder() {
        this.routerFunctions = new ArrayList<>();
    }

    @Override
    public CoreEndpointsBuilder add(CoreEndpoint endpoint) {
        routerFunctions.add(endpoint.endpoint());
        return this;
    }

    @Override
    public RouterFunction<ServerResponse> build() {
        SpringdocRouteBuilder routeBuilder = SpringdocRouteBuilder.route();
        routerFunctions.forEach((routerFunction) -> {
            routeBuilder.nest(RequestPredicates.path("/api/" + OpenApiConst.CORE_VERSION),
                () -> routerFunction,
                builder -> builder.operationId("CoreEndpoints")
                    .description("Core Endpoint")
                    .tag("/CoreEndpoints")
            );
        });
        routerFunctions.clear();
        return routeBuilder.build();
    }
}
