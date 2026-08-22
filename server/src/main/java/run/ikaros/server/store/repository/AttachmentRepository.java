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

    /**
     * 按类型、父目录与名称查询附件并加行锁（FOR UPDATE），
     * 返回 Flux 以容忍已存在的重复记录，取第一条即可。
     * 仅在事务中生效，用于并发刷新目录时防止重复插入/更新。
     */
    @Query("SELECT * FROM attachment WHERE type = :type AND parent_id = :parentId "
        + "AND name = :name FOR UPDATE")
    Flux<AttachmentEntity> findAllByTypeAndParentIdAndNameForUpdate(
        AttachmentType type, UUID parentId, String name);

    Flux<AttachmentEntity> findAllByParentId(UUID parentId);

    Flux<AttachmentEntity> findAllByTypeAndNameLike(AttachmentType type, String name);

    Mono<AttachmentEntity> findByUrl(String url);

    Mono<Long> countByType(AttachmentType type);
}
