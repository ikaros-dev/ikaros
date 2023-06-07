package run.ikaros.server.plugin.resource;

import static run.ikaros.server.plugin.PluginApplicationContextRegistry.getInstance;

import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.endpoint.CustomEndpoint;
import run.ikaros.server.endpoint.CustomEndpointsBuilder;

/**
 * 插件静态资源代理.
 */
@Component
public class PluginCompositeRouterFunction implements RouterFunction<ServerResponse> {
    private final PluginResourceProxyRouterFunctionRegistry resourceProxyRouterFunctionRegistry;

    public PluginCompositeRouterFunction(
        PluginResourceProxyRouterFunctionRegistry resourceProxyRouterFunctionRegistry) {
        this.resourceProxyRouterFunctionRegistry = resourceProxyRouterFunctionRegistry;
    }

    @Override
    public Mono<HandlerFunction<ServerResponse>> route(ServerRequest request) {
        return Flux.fromIterable(routerFunctions())
            .concatMap(routerFunction -> routerFunction.route(request))
            .next();
    }

    @Override
    public void accept(@NonNull RouterFunctions.Visitor visitor) {
        routerFunctions().forEach(routerFunction -> routerFunction.accept(visitor));
    }

    private List<RouterFunction<ServerResponse>> routerFunctions() {
        var rawRouterFunctions = getInstance().getPluginApplicationContexts()
            .stream()
            .flatMap(applicationContext -> applicationContext
                .getBeanProvider(RouterFunction.class)
                .orderedStream())
            .map(router -> (RouterFunction<ServerResponse>) router)
            .toList();
        var reverseProxies = resourceProxyRouterFunctionRegistry.getRouterFunctions();

        var endpointBuilder = new CustomEndpointsBuilder();
        getInstance().getPluginApplicationContexts()
            .forEach(context ->
                context.getBeanProvider(CustomEndpoint.class)
                .orderedStream()
                .forEach(endpointBuilder::add));
        var customEndpoint = endpointBuilder.build();

        List<RouterFunction<ServerResponse>> routerFunctions =
            new ArrayList<>(rawRouterFunctions.size() + reverseProxies.size() + 1);
        routerFunctions.addAll(rawRouterFunctions);
        routerFunctions.addAll(reverseProxies);
        routerFunctions.add(customEndpoint);
        return routerFunctions;
    }
}
