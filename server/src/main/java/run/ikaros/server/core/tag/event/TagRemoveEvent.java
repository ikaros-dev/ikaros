package run.ikaros.server.core.tag.event;

import java.time.Clock;
import lombok.Getter;
import run.ikaros.server.store.entity.TagEntity;

@Getter
public class TagRemoveEvent extends TagChangeEvent {

    public TagRemoveEvent(Object source, TagEntity entity) {
        super(source, entity);
    }

    public TagRemoveEvent(Object source, Clock clock, TagEntity entity) {
        super(source, clock, entity);
    }
}
