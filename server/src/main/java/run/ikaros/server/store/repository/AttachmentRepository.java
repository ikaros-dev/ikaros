package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.store.entity.AttachmentEntity;

public interface AttachmentRepository extends BaseRepository<AttachmentEntity> {
    Mono<Boolean> existsByTypeAndParentIdAndName(AttachmentType type, UUID parentId, String name);

    Mono<Boolean> existsByParentIdAndName(UUID parentId, String name);

    Mono<Void> removeByTypeAndParentIdAndName(
        AttachmentType type, UUID parentId, String name);

    Mono<AttachmentEntity> findByTypeAndParentIdAndName(
        AttachmentType type, UUID parentId, String name);

    Flux<AttachmentEntity> findAllByParentId(UUID parentId);

    Flux<AttachmentEntity> findAllByParentIdAndDriverId(UUID parentId, UUID driverId);

    Flux<AttachmentEntity> findAllByTypeAndNameLike(AttachmentType type, String name);

    Mono<AttachmentEntity> findByUrl(String url);

    Mono<Long> countByType(AttachmentType type);

    @Query("""
        select count(*) from attachment
        where type in ('File', 'Driver_File')
          and (deleted = false or deleted is null)
        """)
    Mono<Long> countKnownFiles();

    @Query("""
        select count(*) from attachment
        where type in ('Directory', 'Driver_Directory')
          and (deleted = false or deleted is null)
          and id not in ($1, $2, $3)
          and not (type = 'Driver_Directory' and parent_id = $1)
        """)
    Mono<Long> countKnownFolders(UUID rootDirectoryId, UUID coverDirectoryId,
                                 UUID downloadDirectoryId);
}
