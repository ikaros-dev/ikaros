package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.FolderEntity;

public interface FolderRepository extends R2dbcRepository<FolderEntity, Long> {
    Mono<FolderEntity> findByNameAndParentId(String name, Long parentId);

    Flux<FolderEntity> findAllByNameLikeAndParentId(String name, Long parentId);

    Flux<FolderEntity> findAllByParentId(Long parentId);
}
