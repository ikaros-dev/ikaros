package run.ikaros.server.core.tag.listener;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.search.subject.SubjectSearchService;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.core.tag.event.TagChangeEvent;
import run.ikaros.server.search.IndicesProperties;
import run.ikaros.server.search.subject.ReactiveSubjectDocConverter;
import run.ikaros.server.store.entity.TagEntity;
import run.ikaros.server.store.repository.SubjectRepository;
import run.ikaros.server.store.repository.TagRepository;

@Slf4j
@Component
public class TagChangeEventListener {

    private final IndicesProperties indicesProperties;
    private final SubjectRepository subjectRepository;
    private final TagRepository tagRepository;
    private final SubjectSearchService subjectSearchService;

    /**
     * Construct.
     */
    public TagChangeEventListener(IndicesProperties indicesProperties,
                                  SubjectRepository subjectRepository, TagRepository tagRepository,
                                  SubjectSearchService subjectSearchService) {
        this.indicesProperties = indicesProperties;
        this.subjectRepository = subjectRepository;
        this.tagRepository = tagRepository;
        this.subjectSearchService = subjectSearchService;
    }

    /**
     * 条目对应的标签新增或删除了，则更新对应的索引文档.
     */
    @EventListener(TagChangeEvent.class)
    public Mono<Void> onTagCreateEvent(TagChangeEvent event) {
        if (!indicesProperties.getInitializer().isEnabled()) {
            return Mono.empty();
        }
        TagEntity tagEntity = event.getEntity();
        if (tagEntity == null || tagEntity.getType() != TagType.SUBJECT) {
            return Mono.empty();
        }
        UUID subjectId = tagEntity.getMasterId();
        return subjectRepository.findById(subjectId)
            .flatMap(ReactiveSubjectDocConverter::fromEntity)
            .flatMap(subjectDoc ->
                tagRepository.findAllByTypeAndMasterId(TagType.SUBJECT, subjectDoc.getId())
                    .map(TagEntity::getName).collectList()
                    .map(tags -> {
                        subjectDoc.setTags(tags);
                        return subjectDoc;
                    })
            ).doOnSuccess(subjectDoc -> {
                try {
                    subjectSearchService.updateDocument(List.of(subjectDoc));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).then();
    }
}
