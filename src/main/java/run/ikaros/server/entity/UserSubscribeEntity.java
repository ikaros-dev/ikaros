package run.ikaros.server.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import run.ikaros.server.enums.SubscribeProgress;
import run.ikaros.server.enums.SubscribeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Data
@Entity
@Table(name = "user_subscribe")
@EqualsAndHashCode(callSuper = true)
public class UserSubscribeEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private SubscribeType type;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    private SubscribeProgress progress;

    private String additional;

}
