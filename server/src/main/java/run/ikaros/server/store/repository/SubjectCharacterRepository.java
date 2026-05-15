package run.ikaros.server.store.repository;

import java.util.UUID;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.SubjectCharacterEntity;

public interface SubjectCharacterRepository
    extends BaseRepository<SubjectCharacterEntity> {
    Mono<SubjectCharacterEntity> findBySubjectIdAndCharacterId(UUID subjectId, UUID characterId);
}
