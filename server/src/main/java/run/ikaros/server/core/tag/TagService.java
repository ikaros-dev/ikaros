package run.ikaros.server.core.tag;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.tag.AttachmentTag;
import run.ikaros.api.core.tag.SubjectTag;
import run.ikaros.api.core.tag.Tag;
import run.ikaros.api.store.enums.TagType;

public interface TagService {
    Flux<Tag> findAll(TagType type, UUID masterId, UUID userId, String name);

    Flux<SubjectTag> findSubjectTags(UUID subjectId);

    Flux<AttachmentTag> findAttachmentTags(UUID attachmentId);

    Mono<Tag> create(Tag tag);

    Mono<Void> remove(TagType type, UUID masterId, String name);

    Mono<Void> removeById(UUID tagId);

}
