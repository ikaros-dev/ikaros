package run.ikaros.server.custom.scheme;

import org.springframework.context.ApplicationEvent;

public class SchemeInitializedEvent extends ApplicationEvent {

    public SchemeInitializedEvent(Object source) {
        super(source);
    }

}
