package run.ikaros.server.core.subject;

import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.warp.PagingWrap;

public interface SubjectService {
    Mono<Subject> findById(Long id);

    Mono<Subject> findByBgmId(Long bgmtvId);

    @Transactional
    Mono<Subject> save(Subject subject);

    @Transactional
    Mono<Void> deleteById(Long id);

    Mono<PagingWrap<Subject>> findAllByPageable(PagingWrap<Subject> pagingWrap);

}
