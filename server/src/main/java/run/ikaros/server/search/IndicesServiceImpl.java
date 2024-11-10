package run.ikaros.server.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.api.search.subject.SubjectSearchService;
import run.ikaros.server.search.subject.ReactiveSubjectDocConverter;
import run.ikaros.server.store.repository.SubjectRepository;

@Slf4j
@Service
public class IndicesServiceImpl implements IndicesService {

    private final SubjectRepository subjectRepository;
    private final SubjectSearchService subjectSearchService;

    /**
     * Construct.
     */
    public IndicesServiceImpl(
        SubjectRepository subjectRepository,
        SubjectSearchService subjectSearchService) {
        this.subjectRepository = subjectRepository;
        this.subjectSearchService = subjectSearchService;
    }


    @Override
    public Mono<Void> rebuildSubjectIndices() {
        return subjectRepository.findAll()
            .flatMap(ReactiveSubjectDocConverter::fromEntity)
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
}
