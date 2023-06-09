package run.ikaros.server.core.plugin;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.server.endpoint.CoreEndpoint;

@Component
public class PluginCoreEndpoint implements CoreEndpoint {
    private final PluginService pluginService;

    public PluginCoreEndpoint(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/Plugin";
        return SpringdocRouteBuilder.route()
            .PUT("/plugin/{name}/state/start", this::startPluginById,
                builder -> builder.operationId("StartPluginById")
                    .tag(tag)
                    .description("Start plugin by id(name).")
                    .parameter(Builder.parameterBuilder()
                        .name("name").in(ParameterIn.PATH)
                        .description("Name of plugin, this is id also."))
                    .response(org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder()
                        .responseCode("200")
                        .description(
                            "Response true is start success, false is request or start fail.")
                        .implementation(Boolean.class)))
            .PUT("/plugin/{name}/state/stop", this::stopPluginById,
                builder -> builder.operationId("StopPluginById")
                    .tag(tag)
                    .description("Stop plugin by id(name).")
                    .parameter(Builder.parameterBuilder()
                        .name("name").in(ParameterIn.PATH)
                        .description("Name of plugin, this is id also."))
                    .response(org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder()
                        .responseCode("200")
                        .description("Response true is stop success, "
                            + "false is request or stop fail.")
                        .implementation(Boolean.class)))
            .PUT("/plugin/{name}/state/reload", this::reloadPluginById,
                builder -> builder.operationId("ReloadPluginById")
                    .tag(tag)
                    .description("Reload plugin by id(name).")
                    .parameter(Builder.parameterBuilder()
                        .name("name").in(ParameterIn.PATH)
                        .description("Name of plugin, this is id also."))
                    .response(org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder()
                        .responseCode("200")
                        .description("Response true is reload success, "
                            + "false is request or reload fail.")
                        .implementation(Boolean.class)))
            .build();

    }


    Mono<ServerResponse> startPluginById(ServerRequest request) {
        String pluginName = request.pathVariable("name");
        return pluginService.start(pluginName)
            .flatMap(isSuccess -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(isSuccess))
            .onErrorResume(NotFoundException.class, err -> ServerResponse.notFound().build());
    }

    Mono<ServerResponse> stopPluginById(ServerRequest request) {
        String pluginName = request.pathVariable("name");
        return pluginService.stop(pluginName)
            .flatMap(isSuccess -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(isSuccess))
            .onErrorResume(NotFoundException.class, err -> ServerResponse.notFound().build());
    }

    Mono<ServerResponse> reloadPluginById(ServerRequest request) {
        String pluginName = request.pathVariable("name");
        return pluginService.reload(pluginName)
            .flatMap(isSuccess -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(isSuccess))
            .onErrorResume(NotFoundException.class, err -> ServerResponse.notFound().build());
    }
}
