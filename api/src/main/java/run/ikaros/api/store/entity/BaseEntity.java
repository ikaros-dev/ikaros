package run.ikaros.api.store.entity;


import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;
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
    private Long id;

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
    private Long createUid;

    /**
     * record create time.
     */
    // @CreatedDate
    @Column("create_time")
    private LocalDateTime createTime;

    /**
     * record last modified user id.
     */
    // @LastModifiedBy
    @Column("update_uid")
    private Long updateUid;

    /**
     * record last modified time.
     */
    // @LastModifiedDate
    @Column("update_time")
    private LocalDateTime updateTime;

    /**
     * optimistic lock version field.
     */
    @Version
    @Column("ol_version")
    private Long optimisticLockVersion;

}
