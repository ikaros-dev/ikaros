package run.ikaros.server.store.entity;


import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;

/**
 * base entity.
 *
 * @author li-guohao
 */
@Data
@Accessors(chain = true)
// todo impl @EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    /**
     * base entity id, generate by identity.
     */
    @Id
    private @Nullable UUID id;

    /**
     * record status, it is logic delete field, has deleted is true, normal is false.
     */
    @Column("delete_status")
    private Boolean deleteStatus = false;

    /**
     * create record user id.
     */
    // @CreatedBy
    @Column("create_uid")
    private @Nullable UUID createUid;

    /**
     * record create time.
     */
    // @CreatedDate
    @Column("create_time")
    private @Nullable LocalDateTime createTime;

    /**
     * record last modified user id.
     */
    // @LastModifiedBy
    @Column("update_uid")
    private @Nullable UUID updateUid;

    /**
     * record last modified time.
     */
    // @LastModifiedDate
    @Column("update_time")
    private @Nullable LocalDateTime updateTime;

    /**
     * optimistic lock version field.
     */
    @Version
    @Column("ol_version")
    private @Nullable Long optimisticLockVersion;

}
