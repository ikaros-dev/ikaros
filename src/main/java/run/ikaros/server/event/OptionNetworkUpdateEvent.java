package run.ikaros.server.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.server.model.dto.OptionNetworkDTO;

@Getter
public class OptionNetworkUpdateEvent extends ApplicationEvent {
    private final OptionNetworkDTO optionNetworkDTO;

    public OptionNetworkUpdateEvent(Object source, OptionNetworkDTO optionNetworkDTO) {
        super(source);
        this.optionNetworkDTO = optionNetworkDTO;
    }
}
