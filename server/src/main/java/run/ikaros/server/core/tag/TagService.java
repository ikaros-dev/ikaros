package run.ikaros.server.core.tag;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.tag.SubjectTag;
import run.ikaros.api.core.tag.Tag;
import run.ikaros.api.store.enums.TagType;

public interface TagService {
    Flux<Tag> findAll(TagType type, Long masterId, Long userId, String name);

    Flux<SubjectTag> findSubjectTags(Long subjectId);

    Mono<Tag> create(Tag tag);

    Mono<Void> remove(TagType type, Long masterId, String name);

    Mono<Void> removeById(Long tagId);

}
