package cn.liguohao.ikaros.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.util.Date;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author li-guohao
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    /**
     * 主键ID自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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
    @Column(name = "crete_time")
    private Date creteTime;

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

    /**
     * 乐观锁版本字段，保留字段，暂未启用
     */
    @Transient
    private Long version = -1L;

    public Long getId() {
        return id;
    }

    public BaseEntity setId(Long id) {
        this.id = id;
        return this;
    }

    public Boolean getStatus() {
        return status;
    }

    public BaseEntity setStatus(Boolean status) {
        this.status = status;
        return this;
    }

    public Long getCreateUid() {
        return createUid;
    }

    public BaseEntity setCreateUid(Long createUid) {
        this.createUid = createUid;
        return this;
    }

    public Date getCreteTime() {
        return creteTime;
    }

    public BaseEntity setCreteTime(Date creteTime) {
        this.creteTime = creteTime;
        return this;
    }

    public Long getUpdateUid() {
        return updateUid;
    }

    public BaseEntity setUpdateUid(Long updateUid) {
        this.updateUid = updateUid;
        return this;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public BaseEntity setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public Long getVersion() {
        return version;
    }

    public BaseEntity setVersion(Long version) {
        this.version = version;
        return this;
    }
}
