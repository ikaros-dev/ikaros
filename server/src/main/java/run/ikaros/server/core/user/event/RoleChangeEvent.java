package run.ikaros.server.core.user.event;

import java.time.Clock;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.store.entity.RoleEntity;

@Getter
public class RoleChangeEvent extends ApplicationEvent {
    private final RoleEntity roleEntity;

    public RoleChangeEvent(Object source, RoleEntity roleEntity) {
        super(source);
        this.roleEntity = roleEntity;
    }

    public RoleChangeEvent(Object source, Clock clock, RoleEntity roleEntity) {
        super(source, clock);
        this.roleEntity = roleEntity;
    }
}
