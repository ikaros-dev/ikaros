package run.ikaros.server.core.attachment.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.core.attachment.VideoSubtitle;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.core.attachment.vo.PostAttachmentRelationsParam;

public interface AttachmentRelationService {
    Flux<AttachmentRelation> findAllByTypeAndAttachmentId(AttachmentRelationType type,
                                                          Long attachmentId);

    Flux<VideoSubtitle> findAttachmentVideoSubtitles(Long attachmentId);

    Mono<AttachmentRelation> putAttachmentRelation(AttachmentRelation attachmentRelation);

    Flux<AttachmentRelation> putAttachmentRelations(
        PostAttachmentRelationsParam postAttachmentRelationsParam);

    Mono<AttachmentRelation> deleteAttachmentRelation(AttachmentRelation attachmentRelation);
}
