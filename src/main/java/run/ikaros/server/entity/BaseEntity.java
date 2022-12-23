package run.ikaros.server.entity;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.Date;

/**
 * @author li-guohao
 */
@Data
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    /**
     * 主键ID自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * status;记录状态: false-已删除 true-正常
     */
    private Boolean status = true;

    /**
     * 记录创建用户
     */
    @CreatedBy
    @Column(name = "create_uid")
    private Long createUid;

    /**
     * 记录创建时间
     */
    @CreatedDate
    @Column(name = "create_time")
    private Date createTime;

    /**
     * 记录最后更新用户
     */
    @LastModifiedBy
    @Column(name = "update_uid")
    private Long updateUid;

    /**
     * 记录最后更新时间
     */
    @LastModifiedDate
    @Column(name = "update_time")
    private Date updateTime;


}
