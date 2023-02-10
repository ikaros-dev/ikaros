package run.ikaros.server.core.subject;

import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

public interface SubjectService {
    Mono<Subject> findById(Long id);

    Mono<Subject> findByBgmId(Long bgmtvId);

    @Transactional
    Mono<Subject> save(Subject subject);

    @Transactional
    Mono<Void> deleteById(Long id);


}
