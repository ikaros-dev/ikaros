package run.ikaros.server.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.api.search.subject.SubjectDoc;
import run.ikaros.api.search.subject.SubjectSearchService;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.search.subject.ReactiveSubjectDocConverter;
import run.ikaros.server.store.entity.TagEntity;
import run.ikaros.server.store.repository.SubjectRepository;
import run.ikaros.server.store.repository.TagRepository;

@Slf4j
@Service
public class IndicesServiceImpl implements IndicesService {

    private final SubjectRepository subjectRepository;
    private final TagRepository tagRepository;
    private final SubjectSearchService subjectSearchService;

    /**
     * Construct.
     */
    public IndicesServiceImpl(
        SubjectRepository subjectRepository, TagRepository tagRepository,
        SubjectSearchService subjectSearchService) {
        this.subjectRepository = subjectRepository;
        this.tagRepository = tagRepository;
        this.subjectSearchService = subjectSearchService;
    }


    @Override
    public Mono<Void> rebuildSubjectIndices() {
        return subjectRepository.findAll()
            .flatMap(ReactiveSubjectDocConverter::fromEntity)
            .flatMap(this::fetchSubTags)
            .collectList()
            .handle((subjectDocs, sink) -> {
                try {
                    subjectSearchService.rebuild(subjectDocs);
                } catch (Exception e) {
                    log.error("Rebuild subject indices fail, msg: {}", e.getMessage(), e);
                }
            })
            .then();
    }

    private Mono<SubjectDoc> fetchSubTags(SubjectDoc subjectDoc) {
        return tagRepository.findAllByTypeAndMasterId(TagType.SUBJECT, subjectDoc.getId())
            .map(TagEntity::getName)
            .collectList()
            .map(tags -> {
                subjectDoc.setTags(tags);
                return subjectDoc;
            });
    }
}
