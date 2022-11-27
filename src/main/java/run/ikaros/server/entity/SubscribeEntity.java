package run.ikaros.server.entity;

import run.ikaros.server.enums.SubscribeType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Entity
@Table(name = "subscribe")
public class SubscribeEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscribeType type;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    private String notes;

    public Long getUserId() {
        return userId;
    }

    public SubscribeEntity setUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public SubscribeType getType() {
        return type;
    }

    public SubscribeEntity setType(SubscribeType type) {
        this.type = type;
        return this;
    }

    public Long getTargetId() {
        return targetId;
    }

    public SubscribeEntity setTargetId(Long targetId) {
        this.targetId = targetId;
        return this;
    }

    public String getNotes() {
        return notes;
    }

    public SubscribeEntity setNotes(String notes) {
        this.notes = notes;
        return this;
    }
}
