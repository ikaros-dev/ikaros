package run.ikaros.server.core.user.event;

import java.time.Clock;
import run.ikaros.server.store.entity.RoleEntity;

public class RoleRemoveEvent extends RoleChangeEvent {

    public RoleRemoveEvent(Object source, RoleEntity roleEntity) {
        super(source, roleEntity);
    }

    public RoleRemoveEvent(Object source, Clock clock, RoleEntity roleEntity) {
        super(source, clock, roleEntity);
    }
}
