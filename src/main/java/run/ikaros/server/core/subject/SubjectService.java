package run.ikaros.server.core.subject;

import reactor.core.publisher.Mono;

public interface SubjectService {
    Mono<Subject> findById(Long subjectId);

    Mono<Subject> findByBgmId(Long bgmtvId);
}
