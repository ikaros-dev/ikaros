package run.ikaros.server.core.attachment.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.core.attachment.service.AttachmentRelationService;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class AttachmentRelationEndpoint implements CoreEndpoint {
    private final AttachmentRelationService attachmentRelationService;

    public AttachmentRelationEndpoint(AttachmentRelationService attachmentRelationService) {
        this.attachmentRelationService = attachmentRelationService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/attachment/relation";
        return SpringdocRouteBuilder.route()
            .GET("/attachment/relations", this::findAttachmentRelations,
                builder -> builder
                    .operationId("FindAttachmentRelations")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("attachmentId")
                        .description("Attachment ID")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder().name("relationType")
                        .description("Relation type")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .implementation(AttachmentRelationType.class))
                    .response(responseBuilder().implementationArray(AttachmentRelation.class)))

            .build();
    }

    Mono<ServerResponse> findAttachmentRelations(ServerRequest request) {
        String attachmentId = request.queryParam("attachmentId").orElse("");
        String relationType = request.queryParam("relationType").orElse("");
        if (!StringUtils.hasText(attachmentId) || !StringUtils.hasText(relationType)) {
            return ServerResponse.badRequest().bodyValue("request params incorrect.");
        }
        long attachmentIdL;
        AttachmentRelationType relationTypeE;
        try {
            attachmentIdL = Long.parseLong(attachmentId);
            relationTypeE = AttachmentRelationType.valueOf(relationType);
        } catch (IllegalArgumentException illegalArgumentException) {
            return ServerResponse.badRequest().bodyValue("request params incorrect.");
        }
        return attachmentRelationService.findAllByTypeAndAttachmentId(relationTypeE, attachmentIdL)
            .collectList()
            .flatMap(attachmentRelations -> ServerResponse.ok().bodyValue(attachmentRelations))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

}
