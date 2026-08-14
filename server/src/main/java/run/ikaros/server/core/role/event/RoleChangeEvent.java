package run.ikaros.server.core.role.event;

import java.time.Clock;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.store.entity.RoleEntity;

@Getter
public class RoleChangeEvent extends ApplicationEvent {
    private final @Nullable RoleEntity roleEntity;

    public RoleChangeEvent(Object source, @Nullable RoleEntity roleEntity) {
        super(source);
        this.roleEntity = roleEntity;
    }

    public RoleChangeEvent(Object source, Clock clock, @Nullable RoleEntity roleEntity) {
        super(source, clock);
        this.roleEntity = roleEntity;
    }
}
