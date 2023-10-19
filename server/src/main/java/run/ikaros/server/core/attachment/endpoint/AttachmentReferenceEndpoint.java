package run.ikaros.server.core.attachment.endpoint;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.attachment.AttachmentReference;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.core.attachment.service.AttachmentReferenceService;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class AttachmentReferenceEndpoint implements CoreEndpoint {
    private final AttachmentReferenceService service;

    public AttachmentReferenceEndpoint(AttachmentReferenceService service) {
        this.service = service;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/attachment/reference";
        return SpringdocRouteBuilder.route()
            .PUT("/attachment/reference", this::save,
                builder -> builder.operationId("SaveAttachmentReference")
                    .tag(tag).description("Save attachment reference.")
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(AttachmentReference.class)))

            .DELETE("/attachment/reference/{id}", this::deleteById,
                builder -> builder.tag(tag)
                    .operationId("DeleteAttachmentReference")
                    .parameter(parameterBuilder().name("id")
                        .description("AttachmentReference ID")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class)))

            .GET("/attachment/references", this::findAllByTypeAndAttachmentId,
                builder -> builder.tag(tag)
                    .operationId("FindAllByTypeAndAttachmentId")
                    .parameter(parameterBuilder()
                        .required(true)
                        .name("type").implementation(AttachmentReferenceType.class)
                        .description("AttachmentReference type"))
                    .parameter(parameterBuilder()
                        .required(true)
                        .name("attachmentId").implementation(Long.class)
                        .description("Attachment id")))

            .build();
    }

    private Mono<ServerResponse> save(ServerRequest request) {
        return request.bodyToMono(AttachmentReference.class)
            .flatMap(service::save)
            .flatMap(attachmentReference -> ServerResponse.ok().bodyValue(attachmentReference));
    }

    private Mono<ServerResponse> deleteById(ServerRequest request) {
        String id = request.pathVariable("id");
        return service.removeById(Long.parseLong(id))
            .then(ServerResponse.ok()
                .bodyValue("Delete success"));
    }

    private Mono<ServerResponse> findAllByTypeAndAttachmentId(ServerRequest request) {
        Optional<String> typeOp = request.queryParam("type");
        if (typeOp.isEmpty()) {
            return ServerResponse.badRequest().bodyValue("type must has value.");
        }
        AttachmentReferenceType type = AttachmentReferenceType.valueOf(typeOp.get());

        Optional<String> attachmentIdOp = request.queryParam("attachmentId");
        if (attachmentIdOp.isEmpty()) {
            return ServerResponse.badRequest().bodyValue("attachmentId must has value.");
        }
        Long attachmentId = Long.valueOf(attachmentIdOp.get());

        return service.findAllByTypeAndAttachmentId(type, attachmentId)
            .collectList()
            .flatMap(attachmentReferences -> ServerResponse.ok().bodyValue(attachmentReferences))
            .switchIfEmpty(ServerResponse.notFound().build());
    }
}
