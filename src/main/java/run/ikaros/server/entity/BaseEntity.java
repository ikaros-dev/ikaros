package run.ikaros.server.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

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

    /**
     * 乐观锁版本字段，保留字段，暂未启用
     */
    @JsonIgnore
    @Transient
    private Long version = -1L;

    public void setTimeAndUidWhenCreate(Date time, Long uid) {
        this.setCreateTime(time)
            .setUpdateTime(time)
            .setCreateUid(uid)
            .setUpdateUid(uid);
    }

    public void setTimeAndUidWhenUpdate(Date time, Long uid) {
        this.setUpdateTime(time).setUpdateUid(uid);
    }


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

    public Date getCreateTime() {
        return createTime;
    }

    public BaseEntity setCreateTime(Date createTime) {
        this.createTime = createTime;
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
