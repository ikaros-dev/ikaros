package run.ikaros.server.core.user.event;

import java.time.Clock;
import run.ikaros.server.store.entity.RoleEntity;

public class RoleRelationCreatedEvent extends RoleRelationChangeEvent {

    public RoleRelationCreatedEvent(Object source, RoleEntity roleEntity, Long userId) {
        super(source, roleEntity, userId);
    }

    public RoleRelationCreatedEvent(Object source, Clock clock, RoleEntity roleEntity,
                                    Long userId) {
        super(source, clock, roleEntity, userId);
    }
}
