package run.ikaros.server.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.model.dto.OptionNotifyDTO;

@Getter
public class OptionNotifyUpdateEvent extends ApplicationEvent {
    private final OptionNotifyDTO optionNotifyDTO;


    public OptionNotifyUpdateEvent(Object source, OptionNotifyDTO optionNotifyDTO) {
        super(source);
        this.optionNotifyDTO = optionNotifyDTO;
    }
}
