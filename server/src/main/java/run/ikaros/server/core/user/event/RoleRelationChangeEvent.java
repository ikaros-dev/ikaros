package run.ikaros.server.core.user.event;

import java.time.Clock;
import lombok.Getter;
import run.ikaros.server.store.entity.RoleEntity;

@Getter
public class RoleRelationChangeEvent extends RoleChangeEvent {
    private final Long userId;

    public RoleRelationChangeEvent(Object source, RoleEntity roleEntity, Long userId) {
        super(source, roleEntity);
        this.userId = userId;
    }

    public RoleRelationChangeEvent(Object source, Clock clock, RoleEntity roleEntity, Long userId) {
        super(source, clock, roleEntity);
        this.userId = userId;
    }
}
