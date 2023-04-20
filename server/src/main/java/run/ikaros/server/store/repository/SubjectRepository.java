package run.ikaros.server.store.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.SubjectEntity;

public interface SubjectRepository extends R2dbcRepository<SubjectEntity, Long> {
    Mono<SubjectEntity> findByBgmtvId(Long bgmtvId);

    Mono<Boolean> existsById(Long id);

    Flux<SubjectEntity> findAllBy(Pageable pageable);
}
