package run.ikaros.server.core.tag.event;

import java.time.Clock;
import lombok.Getter;
import run.ikaros.server.store.entity.TagEntity;

@Getter
public class TagCreateEvent extends TagChangeEvent {

    public TagCreateEvent(Object source, TagEntity entity) {
        super(source, entity);
    }

    public TagCreateEvent(Object source, Clock clock, TagEntity entity) {
        super(source, clock, entity);
    }
}
