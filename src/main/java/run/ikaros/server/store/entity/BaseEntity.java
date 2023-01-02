package run.ikaros.server.store.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * base entity.
 *
 * @author li-guohao
 */
@Data
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    /**
     * base entity id, generate by identity.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * record status, it is logic delete, has deleted is false, normal is true.
     */
    private Boolean status = true;

    /**
     * create record user id.
     */
    @CreatedBy
    @Column(name = "create_uid")
    private Long createUid;

    /**
     * record create time.
     */
    @CreatedDate
    @Column(name = "create_time")
    private Date createTime;

    /**
     * record last modified user id.
     */
    @LastModifiedBy
    @Column(name = "update_uid")
    private Long updateUid;

    /**
     * record last modified time.
     */
    @LastModifiedDate
    @Column(name = "update_time")
    private Date updateTime;

    /**
     * optimistic lock field.
     */
    @Version
    private Long version;

}
