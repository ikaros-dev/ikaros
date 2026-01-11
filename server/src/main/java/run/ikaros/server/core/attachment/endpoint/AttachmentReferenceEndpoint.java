package run.ikaros.server.core.attachment.endpoint;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import java.util.Optional;
import java.util.UUID;
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
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.core.attachment.service.AttachmentReferenceService;
import run.ikaros.server.core.attachment.vo.BatchMatchingEpisodeAttachment;
import run.ikaros.server.core.attachment.vo.BatchMatchingSubjectEpisodesAttachment;
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

            .DELETE("/attachment/reference/id", this::deleteById,
                builder -> builder.tag(tag)
                    .operationId("DeleteAttachmentReference")
                    .parameter(parameterBuilder().name("id")
                        .description("AttachmentReference ID")
                        .required(true)
                        .implementation(String.class)))

            .DELETE("/attachment/reference/uk", this::removeByTypeAndAttachmentIdAndReferenceId,
                builder -> builder.operationId("RemoveByTypeAndAttachmentIdAndReferenceId")
                    .tag(tag).description("Remove by type and attachmentId and referenceId")
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(AttachmentReference.class)))

            .DELETE("/attachment/references", this::removeAllByTypeAndReferenceId,
                builder -> builder.operationId("RemoveAllByTypeAndReferenceId")
                    .tag(tag).description("Remove references by type and referenceId")
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(AttachmentReference.class)))


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

            .POST("/attachment/references/subject/episodes",
                this::matchingAttachmentsAndSubjectEpisodes,
                builder -> builder.operationId("MatchingAttachmentsAndSubjectEpisodes")
                    .tag(tag).description("Matching attachments to episodes for single subject, "
                        + "one episode has one attachment ref.")
                    .requestBody(Builder.requestBodyBuilder()
                        .required(true)
                        .description("batch matching episodes and attachments request value object")
                        .implementation(BatchMatchingSubjectEpisodesAttachment.class)))

            .POST("/attachment/references/episode",
                this::matchingAttachmentsForEpisode,
                builder -> builder.operationId("MatchingAttachmentsForEpisode")
                    .tag(tag).description("Matching attachments for single episode, "
                        + "one episode has many attachment refs.")
                    .requestBody(Builder.requestBodyBuilder()
                        .required(true)
                        .description("batch matching episodes and attachments request value object")
                        .implementation(BatchMatchingEpisodeAttachment.class)))

            .build();
    }

    private Mono<ServerResponse> save(ServerRequest request) {
        return request.bodyToMono(AttachmentReference.class)
            .flatMap(service::save)
            .flatMap(attachmentReference -> ServerResponse.ok().bodyValue(attachmentReference));
    }

    private Mono<ServerResponse> deleteById(ServerRequest request) {
        UUID id = UuidV7Utils.fromString(request.queryParam("id").orElse(""));
        return service.removeById(id)
            .then(ServerResponse.ok()
                .bodyValue("Delete success"));
    }

    private Mono<ServerResponse> removeByTypeAndAttachmentIdAndReferenceId(ServerRequest request) {
        return request.bodyToMono(AttachmentReference.class)
            .flatMap(attachmentReference -> service.removeByTypeAndAttachmentIdAndReferenceId(
                attachmentReference.getType(), attachmentReference.getAttachmentId(),
                attachmentReference.getReferenceId()))
            .then(ServerResponse.ok().bodyValue("Delete success."));
    }

    private Mono<ServerResponse> removeAllByTypeAndReferenceId(ServerRequest request) {
        return request.bodyToMono(AttachmentReference.class)
            .flatMap(attachmentReference -> service.removeAllByTypeAndReferenceId(
                attachmentReference.getType(), attachmentReference.getReferenceId()))
            .then(ServerResponse.ok().bodyValue("Delete success."));
    }

    private Mono<ServerResponse> findAllByTypeAndAttachmentId(ServerRequest request) {
        Optional<String> typeOp = request.queryParam("type");
        if (typeOp.isEmpty()) {
            return ServerResponse.badRequest().bodyValue("type must has value.");
        }
        AttachmentReferenceType type = AttachmentReferenceType.valueOf(typeOp.get());

        UUID attachmentId = UuidV7Utils.fromString(request.queryParam("attachmentId").orElse(""));

        return service.findAllByTypeAndAttachmentId(type, attachmentId)
            .collectList()
            .flatMap(attachmentReferences -> ServerResponse.ok().bodyValue(attachmentReferences))
            .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> matchingAttachmentsAndSubjectEpisodes(ServerRequest request) {
        return request.bodyToMono(BatchMatchingSubjectEpisodesAttachment.class)
            .flatMap(
                batchMatchingSubjectEpisodesAttachment ->
                    service.matchingAttachmentsAndSubjectEpisodes(
                        batchMatchingSubjectEpisodesAttachment.getSubjectId(),
                        batchMatchingSubjectEpisodesAttachment.getAttachmentIds(),
                        batchMatchingSubjectEpisodesAttachment.getEpisodeGroup()))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> matchingAttachmentsForEpisode(ServerRequest request) {
        return request.bodyToMono(BatchMatchingEpisodeAttachment.class)
            .flatMap(
                batchMatchingEpisodeAttachment ->
                    service.matchingAttachmentsForEpisode(
                        batchMatchingEpisodeAttachment.getEpisodeId(),
                        batchMatchingEpisodeAttachment.getAttachmentIds()))
            .then(ServerResponse.ok().build());
    }
}
