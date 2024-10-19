package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.SubjectCharacterEntity;

public interface SubjectCharacterRepository
    extends R2dbcRepository<SubjectCharacterEntity, Long> {
    Mono<SubjectCharacterEntity> findBySubjectIdAndCharacterId(Long subjectId, Long characterId);
}
