package run.ikaros.server.core.plugin;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springframework.web.reactive.function.BodyExtractors.toMultipartData;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static run.ikaros.api.infra.model.ResponseResult.success;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.infra.model.ResponseResult;
import run.ikaros.server.endpoint.CoreEndpoint;

@Component
public class PluginCoreEndpoint implements CoreEndpoint {
    private final PluginService pluginService;

    public PluginCoreEndpoint(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/plugin";
        return SpringdocRouteBuilder.route()
            // .PUT("/plugin/{name}", this::operatePluginById,
            //     builder -> builder.operationId("OperatePluginById")
            //         .tag(tag)
            //         .description("Operate plugin by id(name).")
            //         .parameter(Builder.parameterBuilder().required(true)
            //             .name("name").in(ParameterIn.PATH)
            //             .description("Name of plugin, this is id also."))
            //         .parameter(Builder.parameterBuilder().required(true)
            //             .name("operate").in(ParameterIn.DEFAULT)
            //             .description("Operate of plugin.")
            //             .implementation(PluginOperate.class))
            //         .response(org.springdoc.core.fn.builders.apiresponse.Builder
            //         .responseBuilder()
            //             .responseCode("200")
            //             .description(
            //                 "Response true is operate success,
            //                 false is request or operate fail.")
            //             .implementation(Boolean.class))
            //                    .response(responseBuilder()
            //                        .implementation(ResponseResult.class)))

            .PUT("/plugin/{name}/state", this::operatePluginStateById,
                builder -> builder.operationId("OperatePluginStateById")
                    .tag(tag)
                    .description("Operate plugin state by id(name).")
                    .parameter(Builder.parameterBuilder().required(true)
                        .name("name").in(ParameterIn.PATH)
                        .description(
                            "Name of plugin, this is id also. "
                                + "if operate all plugins, please set value is [ALL]. "))
                    .parameter(Builder.parameterBuilder().required(true)
                        .name("operate").in(ParameterIn.DEFAULT)
                        .description("Operate of plugin state.")
                        .implementation(PluginStateOperate.class))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))

            .PUT("/plugin/{name}/state/start", this::startPluginById,
                builder -> builder.operationId("StartPluginById")
                    .tag(tag)
                    .description("Start plugin by id(name).")
                    .parameter(Builder.parameterBuilder()
                        .name("name").in(ParameterIn.PATH)
                        .description("Name of plugin, this is id also."))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))

            .PUT("/plugin/{name}/state/stop", this::stopPluginById,
                builder -> builder.operationId("StopPluginById")
                    .tag(tag)
                    .description("Stop plugin by id(name).")
                    .parameter(Builder.parameterBuilder()
                        .name("name").in(ParameterIn.PATH)
                        .description("Name of plugin, this is id also."))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))

            .PUT("/plugin/{name}/state/reload", this::reloadPluginById,
                builder -> builder.operationId("ReloadPluginById")
                    .tag(tag)
                    .description("Reload plugin by id(name).")
                    .parameter(Builder.parameterBuilder()
                        .name("name").in(ParameterIn.PATH)
                        .description("Name of plugin, this is id also."))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))

            .POST("/plugin/install/file",
                contentType(MediaType.MULTIPART_FORM_DATA),
                this::installPlugin,
                builder -> builder.operationId("InstallPluginByFile")
                    .tag(tag)
                    .description("Install plugin by upload jar file.")
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .schema(schemaBuilder().required(true)
                                .name("file").description("Plugin jar file.")
                                .implementation(UploadRequest.class))
                        ))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))

            .POST("/plugin/upgrade/file/{pluginId}",
                contentType(MediaType.MULTIPART_FORM_DATA),
                this::upgradePlugin,
                builder -> builder.operationId("UpgradePluginByFile")
                    .tag(tag)
                    .description("Upgrade plugin by upload jar file.")
                    .parameter(Builder.parameterBuilder()
                        .name("pluginId")
                        .in(ParameterIn.PATH)
                        .description("Plugin id(name).")
                        .implementation(String.class))
                    .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                            .mediaType(MediaType.MULTIPART_FORM_DATA_VALUE)
                            .schema(schemaBuilder().required(true)
                                .name("file").description("Plugin jar file.")
                                .implementation(UploadRequest.class))
                        ))
                    .response(responseBuilder()
                        .implementation(ResponseResult.class)))

            .build();

    }

    public interface UploadRequest {
        FilePart getFile();
    }

    public record DefaultUploadRequest(MultiValueMap<String, Part> formData)
        implements UploadRequest {
        /**
         * Get file form data.
         *
         * @return file part.
         */
        @Override
        public FilePart getFile() {
            if (formData.getFirst("file") instanceof FilePart file) {
                return file;
            }
            throw new ServerWebInputException("Invalid part of file");
        }
    }

    Mono<ServerResponse> installPlugin(ServerRequest request) {
        return request.body(toMultipartData())
            .map(DefaultUploadRequest::new)
            .map(DefaultUploadRequest::getFile)
            .flatMap(pluginService::install)
            .then(ok().bodyValue(success()));
    }

    Mono<ServerResponse> upgradePlugin(ServerRequest request) {

        String pluginId = request.pathVariable("pluginId");

        return request.body(toMultipartData())
            .map(DefaultUploadRequest::new)
            .map(DefaultUploadRequest::getFile)
            .flatMap(filePart -> pluginService.upgrade(pluginId, filePart))
            .then(ok().bodyValue(success()));
    }

    Mono<ServerResponse> operatePluginStateById(ServerRequest request) {
        String pluginName = request.pathVariable("name");
        return Mono.justOrEmpty(request.queryParam("operate"))
            .map(PluginStateOperate::valueOf)
            .flatMap(pluginStateOperate ->
                pluginService.operateState(pluginName, pluginStateOperate))
            .flatMap(pluginState -> ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(success(pluginState)));
    }


    Mono<ServerResponse> startPluginById(ServerRequest request) {
        String pluginName = request.pathVariable("name");
        return pluginService.start(pluginName)
            .flatMap(isSuccess -> ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(success(isSuccess)));
    }

    Mono<ServerResponse> stopPluginById(ServerRequest request) {
        String pluginName = request.pathVariable("name");
        return pluginService.stop(pluginName)
            .flatMap(isSuccess -> ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(success(isSuccess)));
    }

    Mono<ServerResponse> reloadPluginById(ServerRequest request) {
        String pluginName = request.pathVariable("name");
        return pluginService.reload(pluginName)
            .flatMap(isSuccess -> ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(success(isSuccess)));
    }
}
