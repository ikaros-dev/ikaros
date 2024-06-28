package run.ikaros.server.core.role.event;

import java.time.Clock;
import run.ikaros.server.store.entity.RoleEntity;

public class RoleRelationRemoveEvent extends RoleRelationChangeEvent {

    public RoleRelationRemoveEvent(Object source, RoleEntity roleEntity, Long userId) {
        super(source, roleEntity, userId);
    }

    public RoleRelationRemoveEvent(Object source, Clock clock, RoleEntity roleEntity,
                                   Long userId) {
        super(source, clock, roleEntity, userId);
    }
}
