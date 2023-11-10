package run.ikaros.server.core.tag;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.tag.SubjectTag;
import run.ikaros.api.core.tag.Tag;

public interface TagService {
    Flux<Tag> findAll(Tag tag);

    Flux<SubjectTag> findSubjectTags(Long subjectId);

    Mono<Tag> create(Tag tag);

    Mono<Tag> remove(Tag tag);

    Mono<Tag> removeById(Long tagId);

}
