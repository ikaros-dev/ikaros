package run.ikaros.server.core.attachment.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.server.core.attachment.service.AttachmentDriverService;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class AttachmentDriverEndpoint implements CoreEndpoint {
    private final AttachmentDriverService service;

    public AttachmentDriverEndpoint(AttachmentDriverService service) {
        this.service = service;
    }


    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/attachment/driver";
        return SpringdocRouteBuilder.route()
            .PUT("/attachment/driver", this::save,
                builder -> builder.operationId("SaveAttachmentDriver")
                    .tag(tag).description("Save attachment driver.")
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(AttachmentDriver.class))
                    .response(responseBuilder().implementation(AttachmentDriver.class))
            )

            .PUT("/attachment/driver/enable/id/{id}", this::enableDriver,
                builder -> builder.operationId("EnableDriver")
                    .tag(tag).description("Enable attachment driver.")
                    .parameter(parameterBuilder().name("id")
                        .in(ParameterIn.PATH)
                        .description("AttachmentDriver ID")
                        .required(true)
                        .implementation(Long.class))
            )

            .PUT("/attachment/driver/disable/id/{id}", this::disableDriver,
                builder -> builder.operationId("EnableDriver")
                    .tag(tag).description("Disable attachment driver.")
                    .parameter(parameterBuilder().name("id")
                        .in(ParameterIn.PATH)
                        .description("AttachmentDriver ID")
                        .required(true)
                        .implementation(Long.class))
            )

            .DELETE("/attachment/driver/id/{id}", this::deleteById,
                builder -> builder.tag(tag)
                    .operationId("DeleteAttachmentDriverById")
                    .description("Delete attachment driver by ID.")
                    .parameter(parameterBuilder().name("id")
                        .in(ParameterIn.PATH)
                        .description("AttachmentDriver ID")
                        .required(true)
                        .implementation(Long.class))
            )

            .GET("/attachment/driver/id/{id}", this::getById,
                builder -> builder.tag(tag)
                    .operationId("GetAttachmentDriverById")
                    .description("Get attachment driver by ID.")
                    .parameter(parameterBuilder().name("id")
                        .in(ParameterIn.PATH)
                        .description("AttachmentDriver ID")
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder().implementation(AttachmentDriver.class))
            )

            .DELETE("/attachment/driver/uk", this::deleteByTypeAndName,
                builder -> builder.operationId("DeleteByTypeAndName")
                    .tag(tag).description("Delete attachment driver by type and name.")
                    .parameter(parameterBuilder().name("type")
                        .description("AttachmentDriver type")
                        .required(true)
                        .implementation(AttachmentDriverType.class))
                    .parameter(parameterBuilder().name("name")
                        .description("AttachmentDriver name")
                        .required(true)
                        .implementation(String.class))
            )

            .GET("/attachment/driver/uk", this::getByTypeAndName,
                builder -> builder.operationId("GetByTypeAndName")
                    .tag(tag).description("Get attachment driver by type and name.")
                    .parameter(parameterBuilder().name("type")
                        .description("AttachmentDriver type")
                        .required(true)
                        .implementation(AttachmentDriverType.class))
                    .parameter(parameterBuilder().name("name")
                        .description("AttachmentDriver name")
                        .required(true)
                        .implementation(String.class))
                    .response(responseBuilder()
                        .implementation(AttachmentDriver.class))
            )

            .build();
    }

    private Mono<ServerResponse> save(ServerRequest request) {
        return request.bodyToMono(AttachmentDriver.class)
            .flatMap(service::save)
            .flatMap(attachmentDriver -> ServerResponse.ok().bodyValue(attachmentDriver));
    }

    private Mono<ServerResponse> deleteById(ServerRequest request) {
        String id = request.pathVariable("id");
        return service.removeById(Long.parseLong(id))
            .then(ServerResponse.ok()
                .bodyValue("Delete success"));
    }

    private Mono<ServerResponse> deleteByTypeAndName(ServerRequest request) {
        String name = request.queryParam("name").orElse("-1");
        String type = request.queryParam("type").orElse(null);
        return service.removeByTypeAndName(type, name)
            .then(ServerResponse.ok()
                .bodyValue("Delete success"));
    }

    private Mono<ServerResponse> getById(ServerRequest request) {
        String id = request.pathVariable("id");
        return service.findById(Long.parseLong(id))
            .flatMap(driver -> ServerResponse.ok().bodyValue(driver));
    }

    private Mono<ServerResponse> getByTypeAndName(ServerRequest request) {
        String name = request.queryParam("name").orElse("-1");
        String type = request.queryParam("type").orElse(null);
        return service.findByTypeAndName(type, name)
            .flatMap(driver -> ServerResponse.ok().bodyValue(driver));
    }

    private Mono<ServerResponse> enableDriver(ServerRequest request) {
        String id = request.pathVariable("id");
        return service.enable(Long.valueOf(id))
            .then(ServerResponse.ok()
                .bodyValue("Enable success"));
    }

    private Mono<ServerResponse> disableDriver(ServerRequest request) {
        String id = request.pathVariable("id");
        return service.disable(Long.valueOf(id))
            .then(ServerResponse.ok()
                .bodyValue("Disable success"));
    }

}
