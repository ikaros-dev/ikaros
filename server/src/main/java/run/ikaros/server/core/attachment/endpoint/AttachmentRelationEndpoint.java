package run.ikaros.server.core.attachment.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.core.attachment.VideoSubtitle;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.core.attachment.service.AttachmentRelationService;
import run.ikaros.server.core.attachment.vo.PostAttachmentRelationsParam;
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
            .PUT("/attachment/relation/{masterAttachmentId}", this::putAttachmentRelation,
                builder -> builder
                    .operationId("PutAttachmentRelation")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("masterAttachmentId")
                        .description("Master attachment id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("relAttachmentId")
                        .description("Related attachment id")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("type")
                        .description("Type of attachment")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .implementation(AttachmentRelationType.class))
                    .response(responseBuilder()
                        .description("Attachment Relation.")
                        .implementation(AttachmentRelation.class)))

            .POST("/attachment/relations", this::postAttachmentRelations,
                builder -> builder
                    .operationId("PostAttachmentRelations")
                    .tag(tag)
                    .requestBody(Builder.requestBodyBuilder()
                        .description("Post attachment relations request body.")
                        .implementation(PostAttachmentRelationsParam.class)
                        .required(true))
                    .response(responseBuilder()
                        .description("Attachment Relation List.")
                        .implementationArray(AttachmentRelation.class)))

            .DELETE("/attachment/relation/{masterAttachmentId}", this::deleteAttachmentRelation,
                builder -> builder
                    .operationId("DeleteAttachmentRelation")
                    .tag(tag)
                    .parameter(parameterBuilder()
                        .name("masterAttachmentId")
                        .description("Master attachment id")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("relAttachmentId")
                        .description("Related attachment id")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .implementation(Long.class))
                    .parameter(parameterBuilder()
                        .name("type")
                        .description("Type of attachment")
                        .in(ParameterIn.QUERY)
                        .required(true)
                        .implementation(AttachmentRelationType.class))
                    .response(responseBuilder()
                        .description("Attachment Relation.")
                        .implementation(AttachmentRelation.class)))

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

            .GET("/attachment/relation/videoSubtitle/subtitles/{attachmentId}",
                this::findAttachmentVideoSubtitles,
                builder -> builder.tag(tag).operationId("FindAttachmentVideoSubtitles")
                    .parameter(parameterBuilder()
                        .name("attachmentId")
                        .description("Attachment ID")
                        .in(ParameterIn.PATH)
                        .required(true)
                        .implementation(Long.class))
                    .response(responseBuilder()
                        .implementationArray(VideoSubtitle.class)))

            .build();
    }

    Mono<ServerResponse> putAttachmentRelation(ServerRequest request) {
        String masterAttachmentIdStr = request.pathVariable("masterAttachmentId");
        String relAttachmentIdStr = request.queryParam("relAttachmentId").orElse("");
        String typeStr =
            request.queryParam("type")
                .orElse(AttachmentRelationType.VIDEO_SUBTITLE.name());
        Long masterAttachmentId = Long.valueOf(masterAttachmentIdStr);
        Long relAttachmentId = Long.valueOf(relAttachmentIdStr);
        AttachmentRelationType type = AttachmentRelationType.valueOf(typeStr);
        AttachmentRelation attachmentRelation = AttachmentRelation.builder()
            .attachmentId(masterAttachmentId)
            .type(type)
            .relationAttachmentId(relAttachmentId)
            .build();
        return attachmentRelationService.putAttachmentRelation(attachmentRelation)
            .flatMap(attRel -> ServerResponse.ok().bodyValue(attRel));
    }

    Mono<ServerResponse> deleteAttachmentRelation(ServerRequest request) {
        String masterAttachmentIdStr = request.pathVariable("masterAttachmentId");
        String relAttachmentIdStr = request.queryParam("relAttachmentId").orElse("");
        String typeStr =
            request.queryParam("type")
                .orElse(AttachmentRelationType.VIDEO_SUBTITLE.name());
        Long masterAttachmentId = Long.valueOf(masterAttachmentIdStr);
        Long relAttachmentId = Long.valueOf(relAttachmentIdStr);
        AttachmentRelationType type = AttachmentRelationType.valueOf(typeStr);
        AttachmentRelation attachmentRelation = AttachmentRelation.builder()
            .attachmentId(masterAttachmentId)
            .type(type)
            .relationAttachmentId(relAttachmentId)
            .build();
        return attachmentRelationService.deleteAttachmentRelation(attachmentRelation)
            .flatMap(attRel -> ServerResponse.ok().bodyValue(attRel));
    }

    Mono<ServerResponse> postAttachmentRelations(ServerRequest request) {
        return request.bodyToMono(PostAttachmentRelationsParam.class)
            .flatMap(postAttachmentRelationsParam ->
                attachmentRelationService.putAttachmentRelations(postAttachmentRelationsParam)
                    .collectList())
            .flatMap(attachmentRels -> ServerResponse.ok().bodyValue(attachmentRels));
    }

    Mono<ServerResponse> findAttachmentVideoSubtitles(ServerRequest request) {
        Long attachmentId = Long.parseLong(request.pathVariable("attachmentId"));
        return attachmentRelationService.findAttachmentVideoSubtitles(attachmentId)
            .collectList()
            .flatMap(videoSubtitles -> ServerResponse.ok().bodyValue(videoSubtitles));
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
