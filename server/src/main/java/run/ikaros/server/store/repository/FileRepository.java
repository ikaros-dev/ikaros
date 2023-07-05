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

    Flux<FileEntity> findAllByFolderId(Long folderId);


    Flux<FileEntity> findAllByNameLike(String name, Pageable pageable);

    Mono<Long> countAllByNameLike(String name);

    Flux<FileEntity> findAllByNameLikeAndType(String name, FileType type, Pageable pageable);

    Flux<FileEntity> findAllByNameLikeAndType(String name, FileType type);

    Mono<Long> countAllByNameLikeAndType(String name, FileType type);

    Mono<Boolean> existsByFsPath(String fsPath);


}
