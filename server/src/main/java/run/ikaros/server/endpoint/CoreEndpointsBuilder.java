package run.ikaros.server.endpoint;

import org.springdoc.webmvc.core.fn.SpringdocRouteBuilder;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import run.ikaros.api.constant.OpenApiConst;

import java.util.ArrayList;
import java.util.List;

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
        routerFunctions.forEach((routerFunction) ->
                routeBuilder.nest(RequestPredicates.path("/api/" + OpenApiConst.CORE_VERSION),
                        () -> routerFunction
                ));
        routerFunctions.clear();
        return routeBuilder.build();
    }
}
