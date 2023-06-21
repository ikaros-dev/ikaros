package run.ikaros.server.core.subject;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectOperate;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.subject.service.SubjectService;

@Slf4j
@Component
public class SubjectOperator implements SubjectOperate {
    private final SubjectService subjectService;

    public SubjectOperator(SubjectService subjectService) {
        this.subjectService = subjectService;
    }


    @Override
    public Mono<Subject> findById(Long id) {
        return subjectService.findById(id);
    }

    @Override
    public Flux<Subject> findAllByPageable(PagingWrap<Subject> pagingWrap) {
        return subjectService.findAllByPageable(pagingWrap)
            .map(PagingWrap::getItems)
            .flatMapMany(subjects -> Flux.fromStream(subjects.stream()));
    }


    @Override
    public Mono<Subject> create(Subject subject) {
        return subjectService.create(subject);
    }

    @Override
    public Mono<Void> update(Subject subject) {
        return subjectService.update(subject);
    }

    @Override
    public Mono<Void> removeById(Long id) {
        return subjectService.deleteById(id);
    }
}
