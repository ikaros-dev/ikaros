package run.ikaros.server.core.tag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.tag.SubjectTag;
import run.ikaros.api.core.tag.Tag;
import run.ikaros.server.store.repository.TagRepository;

@Slf4j
@Service
public class DefaultTagService implements TagService {
    private final TagRepository tagRepository;

    public DefaultTagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public Flux<Tag> findAll(Tag tag) {
        Assert.notNull(tag, "'tag' must not null.");

        return null;
    }

    @Override
    public Flux<SubjectTag> findSubjectTags(Long subjectId) {
        return null;
    }

    @Override
    public Mono<Tag> create(Tag tag) {
        return null;
    }

    @Override
    public Mono<Tag> remove(Tag tag) {
        return null;
    }

    @Override
    public Mono<Tag> removeById(Long tagId) {
        return null;
    }
}
