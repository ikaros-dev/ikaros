package run.ikaros.server.custom.router;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.custom.ReactiveCustomClient;
import run.ikaros.server.custom.scheme.CustomScheme;
import run.ikaros.server.custom.scheme.CustomSchemeWatcherManager;
import run.ikaros.server.custom.scheme.CustomSchemeWatcherManager.SchemeRegistered;
import run.ikaros.server.custom.scheme.CustomSchemeWatcherManager.SchemeUnregistered;

public class CustomCompositeRouterFunction implements
    RouterFunction<ServerResponse>, CustomSchemeWatcherManager.SchemeWatcher {

    private final Map<CustomScheme, RouterFunction<ServerResponse>> customRouterFuncMapper;

    private final ReactiveCustomClient client;

    /**
     * Construct.
     */
    public CustomCompositeRouterFunction(ReactiveCustomClient client,
                                         CustomSchemeWatcherManager watcherManager) {
        this.client = client;
        customRouterFuncMapper = new ConcurrentHashMap<>();
        if (watcherManager != null) {
            watcherManager.register(this);
        }
    }

    @Override
    @NonNull
    public Mono<HandlerFunction<ServerResponse>> route(@NonNull ServerRequest request) {
        return Flux.fromIterable(getRouterFunctions())
            .concatMap(routerFunction -> routerFunction.route(request))
            .next();
    }

    @Override
    public void accept(@NonNull RouterFunctions.Visitor visitor) {
        getRouterFunctions().forEach(routerFunction -> routerFunction.accept(visitor));
    }

    private Iterable<RouterFunction<ServerResponse>> getRouterFunctions() {
        return Collections.unmodifiableCollection(customRouterFuncMapper.values());
    }

    @Override
    public void onChange(CustomSchemeWatcherManager.ChangeEvent event) {
        if (event instanceof SchemeRegistered registeredEvent) {
            var scheme = registeredEvent.getNewScheme();
            var factory = new CustomRouterFunctionFactory(scheme, client);
            this.customRouterFuncMapper.put(scheme, factory.create());
        } else if (event instanceof SchemeUnregistered unregisteredEvent) {
            this.customRouterFuncMapper.remove(unregisteredEvent.getDeletedScheme());
        }
    }
}
