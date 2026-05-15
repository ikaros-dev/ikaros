package run.ikaros.server.plugin.resource;

import static org.springframework.http.MediaType.ALL;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static run.ikaros.api.plugin.PluginConst.STATIC_RESOURCE_DIR_CONSOLE;
import static run.ikaros.api.plugin.PluginConst.STATIC_RESOURCE_DIR_STATIC;
import static run.ikaros.server.plugin.resource.BundleResourceUtils.getResourceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.pattern.PathPatternParser;
import run.ikaros.api.plugin.PluginConst;
import run.ikaros.server.infra.utils.PathUtils;
import run.ikaros.server.plugin.IkarosPluginManager;
import run.ikaros.server.plugin.PluginApplicationContext;
import run.ikaros.server.plugin.PluginApplicationContextRegistry;

@Slf4j
@Component
public class PluginResourceProxyRouterFunctionRegistry {

    private final IkarosPluginManager ikarosPluginManager;

    public PluginResourceProxyRouterFunctionRegistry(IkarosPluginManager ikarosPluginManager) {
        this.ikarosPluginManager = ikarosPluginManager;
    }

    /**
     * Get all plugin static resource proxy router function.
     */
    public List<RouterFunction<ServerResponse>> getRouterFunctions() {
        List<RouterFunction<ServerResponse>> result = new ArrayList<>();
        for (PluginApplicationContext pluginApplicationContext :
            PluginApplicationContextRegistry.getInstance()
                .getPluginApplicationContexts()) {
            String pluginId = pluginApplicationContext.getPluginId();
            result.add(buildPluginResourceRouterFunction(pluginId, STATIC_RESOURCE_DIR_CONSOLE));
            result.add(buildPluginResourceRouterFunction(pluginId, STATIC_RESOURCE_DIR_STATIC));
        }
        return result;
    }

    private RouterFunction<ServerResponse> buildPluginResourceRouterFunction(String pluginId,
                                                                             String staticDirName) {
        String routePath = buildRoutePath(pluginId, staticDirName);
        // log.debug("Plugin [{}] resource proxy mapping route path [{}]", pluginId,
        //    routePath);
        return RouterFunctions.route(GET(routePath).and(accept(ALL)),
            request -> {
                Resource resource =
                    loadResourceByFileRule(pluginId, request, staticDirName);
                if (!resource.exists()) {
                    return ServerResponse.notFound().build();
                }
                return ServerResponse.ok()
                    .bodyValue(resource);
            });
    }

    private String buildRoutePath(String pluginId, String staticDirName) {
        return PathUtils.combinePath(PluginConst.assertsRoutePrefix(pluginId), staticDirName)
            + "/**";
    }

    private Resource loadResourceByFileRule(String pluginId, ServerRequest request,
                                            String staticDirName) {

        String routePath = buildRoutePath(pluginId, staticDirName);
        PathContainer pathContainer = PathPatternParser.defaultInstance.parse(routePath)
            .extractPathWithinPattern(PathContainer.parsePath(request.path()));
        String filename = pathContainer.value();

        String filePath = PathUtils.combinePath(staticDirName, filename);
        return Objects.requireNonNull(getResourceLoader(ikarosPluginManager, pluginId))
            .getResource(filePath);
    }


}
