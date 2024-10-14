package run.ikaros.server.core.tag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.tag.SubjectTag;
import run.ikaros.api.core.tag.Tag;
import run.ikaros.api.core.tag.TagOperate;
import run.ikaros.api.store.enums.TagType;

@Slf4j
@Service
public class DefaultTagOperator implements TagOperate {
    private final TagService tagService;

    public DefaultTagOperator(TagService tagService) {
        this.tagService = tagService;
    }

    @Override
    public Flux<Tag> findAll(TagType type, Long masterId, Long userId, String name) {
        return tagService.findAll(type, masterId, userId, name);
    }

    @Override
    public Flux<SubjectTag> findSubjectTags(Long subjectId) {
        return tagService.findSubjectTags(subjectId);
    }

    @Override
    public Mono<Tag> create(Tag tag) {
        return tagService.create(tag);
    }
}
