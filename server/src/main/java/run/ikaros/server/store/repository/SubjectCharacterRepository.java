package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.SubjectCharacterEntity;

public interface SubjectCharacterRepository
    extends R2dbcRepository<SubjectCharacterEntity, UUID> {
    Mono<SubjectCharacterEntity> findBySubjectIdAndCharacterId(UUID subjectId, UUID characterId);
}
