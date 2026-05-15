package run.ikaros.api.core.tag;

import java.util.UUID;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.TagType;

public interface TagOperate extends AllowPluginOperate {

    Flux<Tag> findAll(TagType type, UUID masterId, @Nullable String name);

    Flux<SubjectTag> findSubjectTags(UUID subjectId);

    Mono<Tag> create(Tag tag);
}
