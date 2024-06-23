package run.ikaros.server.core.user.event;

import java.time.Clock;
import run.ikaros.server.store.entity.RoleEntity;

public class RoleCreatedEvent extends RoleChangeEvent {

    public RoleCreatedEvent(Object source, RoleEntity roleEntity) {
        super(source, roleEntity);
    }

    public RoleCreatedEvent(Object source, Clock clock, RoleEntity roleEntity) {
        super(source, clock, roleEntity);
    }
}
