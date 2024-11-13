package run.ikaros.server.core.tag.event;

import java.time.Clock;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.store.entity.TagEntity;

@Getter
public class TagChangeEvent extends ApplicationEvent {
    private final TagEntity entity;

    public TagChangeEvent(Object source, TagEntity entity) {
        super(source);
        this.entity = entity;
    }

    public TagChangeEvent(Object source, Clock clock, TagEntity entity) {
        super(source, clock);
        this.entity = entity;
    }
}
