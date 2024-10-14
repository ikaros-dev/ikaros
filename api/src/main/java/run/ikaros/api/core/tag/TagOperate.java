package run.ikaros.api.core.tag;

import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.TagType;

public interface TagOperate extends AllowPluginOperate {

    Flux<Tag> findAll(TagType type, Long masterId, @Nullable String name);

    Flux<SubjectTag> findSubjectTags(Long subjectId);

    Mono<Tag> create(Tag tag);
}
