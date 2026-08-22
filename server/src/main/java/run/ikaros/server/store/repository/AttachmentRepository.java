package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * 仅在事务中生效，用于并发刷新目录时防止重复插入/更新。.
     */
    @Query("SELECT * FROM attachment WHERE type = :type AND parent_id = :parentId "
        + "AND name = :name FOR UPDATE")
    Flux<AttachmentEntity> findAllByTypeAndParentIdAndNameForUpdate(
        AttachmentType type, UUID parentId, String name);

    Flux<AttachmentEntity> findAllByParentId(UUID parentId);

    Flux<AttachmentEntity> findAllByTypeAndNameLike(AttachmentType type, String name);

    Mono<AttachmentEntity> findByUrl(String url);

    Mono<Long> countByType(AttachmentType type);

    /**
     * 按 (type, parent_id, name) 原子 upsert 附件：
     * 同名记录存在则更新、不存在则插入，避免并发刷新目录导致重复插入与唯一键冲突。.
     *
     * @param entity 待写入的附件实体
     * @return 写入完成的空 Mono
     */
    @Modifying
    @Query("""
        INSERT INTO attachment (id, parent_id, type, url, path, fs_path, name
        , size, update_time, deleted, driver_id, sha1)
        VALUES (:#{#entity.id}, :#{#entity.parentId}, :#{#entity.type}, :#{#entity.url},
                :#{#entity.path}, :#{#entity.fsPath}, :#{#entity.name}, :#{#entity.size},
                :#{#entity.updateTime}, :#{#entity.deleted}, :#{#entity.driverId}, :#{#entity.sha1})
        ON CONFLICT (type, parent_id, name) DO UPDATE SET
            url = EXCLUDED.url,
            path = EXCLUDED.path,
            fs_path = EXCLUDED.fs_path,
            size = EXCLUDED.size,
            update_time = EXCLUDED.update_time,
            deleted = EXCLUDED.deleted,
            driver_id = EXCLUDED.driver_id,
            sha1 = EXCLUDED.sha1
        """)
    Mono<Void> upsert(@Param("entity") AttachmentEntity entity);
}
