package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.FileRelationType;
import run.ikaros.server.store.entity.FileRelationEntity;

public interface FileRelationRepository extends R2dbcRepository<FileRelationEntity, Long> {
    Flux<FileRelationEntity> findAllByRelationTypeAndFileId(FileRelationType relationType,
                                                            Long fileId);

    Mono<FileRelationEntity> findByRelationTypeAndFileIdAndRelationFileId(
        FileRelationType relationType, Long fileId, Long relationFileId
    );
}
