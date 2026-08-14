package run.ikaros.server.core.tag;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.tag.AttachmentTag;
import run.ikaros.api.core.tag.SubjectTag;
import run.ikaros.api.core.tag.Tag;
import run.ikaros.api.store.enums.TagType;

public interface TagService {
    Flux<Tag> findAll(@Nullable TagType type, @Nullable UUID masterId,
                      @Nullable UUID userId, @Nullable String name);

    Flux<SubjectTag> findSubjectTags(@Nullable UUID subjectId);

    Flux<AttachmentTag> findAttachmentTags(@Nullable UUID attachmentId);

    Mono<Tag> create(Tag tag);

    Mono<Void> remove(TagType type, @Nullable UUID masterId, String name, UUID userId);

    Mono<Void> removeById(@Nullable UUID tagId);

}
