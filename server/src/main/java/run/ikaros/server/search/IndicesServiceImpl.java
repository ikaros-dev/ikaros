package run.ikaros.server.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.api.search.file.FileSearchService;
import run.ikaros.api.search.subject.SubjectSearchService;
import run.ikaros.server.search.file.FileDocConverter;
import run.ikaros.server.search.subject.ReactiveSubjectDocConverter;
import run.ikaros.server.store.repository.FileRepository;
import run.ikaros.server.store.repository.SubjectRepository;

@Slf4j
@Service
public class IndicesServiceImpl implements IndicesService {

    private final FileRepository fileRepository;
    private final FileSearchService fileSearchService;
    private final SubjectRepository subjectRepository;
    private final SubjectSearchService subjectSearchService;

    /**
     * Construct.
     */
    public IndicesServiceImpl(FileRepository fileRepository, FileSearchService fileSearchService,
                              SubjectRepository subjectRepository,
                              SubjectSearchService subjectSearchService) {
        this.fileRepository = fileRepository;
        this.fileSearchService = fileSearchService;
        this.subjectRepository = subjectRepository;
        this.subjectSearchService = subjectSearchService;
    }

    @Override
    public Mono<Void> rebuildFileIndices() {
        return fileRepository.findAll()
            .map(FileDocConverter::fromEntity)
            .limitRate(100000)
            .buffer(100000)
            .doOnNext(fileDocs -> {
                try {
                    fileSearchService.rebuild(fileDocs);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .then();
    }

    @Override
    public Mono<Void> rebuildSubjectIndices() {
        return subjectRepository.findAll()
            .flatMap(ReactiveSubjectDocConverter::fromEntity)
            .limitRate(100)
            .buffer(100)
            .handle((subjectDocs, sink) -> {
                try {
                    subjectSearchService.rebuild(subjectDocs);
                } catch (Exception e) {
                    sink.error(new RuntimeException(e));
                }
            })
            .then();
    }
}
