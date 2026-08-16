package run.ikaros.server.core.role.event;

import java.time.Clock;
import org.jspecify.annotations.Nullable;
import run.ikaros.server.store.entity.RoleEntity;

public class RoleRemoveEvent extends RoleChangeEvent {

    public RoleRemoveEvent(Object source, @Nullable RoleEntity roleEntity) {
        super(source, roleEntity);
    }

    public RoleRemoveEvent(Object source, Clock clock, @Nullable RoleEntity roleEntity) {
        super(source, clock, roleEntity);
    }
}
