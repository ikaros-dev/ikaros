package run.ikaros.server.custom.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import run.ikaros.api.custom.scheme.CustomScheme;

@Getter
public class CustomUpdateEvent extends ApplicationEvent {
    private final CustomScheme scheme;
    private final String name;

    /**
     * Construct.
     */
    public CustomUpdateEvent(Object source, CustomScheme scheme, String name) {
        super(source);
        this.scheme = scheme;
        this.name = name;
    }
}
