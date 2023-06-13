package run.ikaros.server.store.repository;


import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import run.ikaros.api.store.entity.FileEntity;

/**
 * FileEntity repository.
 *
 * @author: li-guohao
 * @see FileEntity
 */
public interface FileRepository extends R2dbcRepository<FileEntity, Long> {
    Flux<FileEntity> findAllBy(Pageable pageable);
}
