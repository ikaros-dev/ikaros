package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.FileRemoteEntity;

public interface FileRemoteRepository extends R2dbcRepository<FileRemoteEntity, Long> {
    Flux<FileRemoteEntity> findAllByFileId(Long fileId);

    Mono<Boolean> existsAllByFileId(Long fileId);

    Flux<FileRemoteEntity> findAllByFileIdAndRemote(Long fileId, String remote);

    Mono<Void> deleteAllByFileId(Long fileId);
}
