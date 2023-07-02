package run.ikaros.server.store.repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.FileType;
import run.ikaros.server.store.entity.FileEntity;

/**
 * FileEntity repository.
 *
 * @author: li-guohao
 * @see FileEntity
 */
public interface FileRepository extends R2dbcRepository<FileEntity, Long> {
    Flux<FileEntity> findAllBy(Pageable pageable);

    Flux<FileEntity> findAllByOriginalNameLikeAndType(String originalName, FileType type,
                                                      Pageable pageable);

    Flux<FileEntity> findAllByNameLike(String name, Pageable pageable);

    Mono<Long> countAllByNameLike(String name);

    Flux<FileEntity> findAllByNameLikeAndType(String name, FileType type, Pageable pageable);

    Mono<Long> countAllByNameLikeAndType(String name, FileType type);

    Mono<Boolean> existsByOriginalPath(String originalPath);

    Mono<FileEntity> findByOriginalPath(String originalPath);
}
