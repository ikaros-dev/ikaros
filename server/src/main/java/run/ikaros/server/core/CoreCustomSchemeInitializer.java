package run.ikaros.server.core;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import run.ikaros.api.core.file.FilePolicy;
import run.ikaros.api.core.file.FileSetting;
import run.ikaros.api.custom.scheme.CustomSchemeManager;
import run.ikaros.api.plugin.custom.Plugin;
import run.ikaros.server.custom.scheme.SchemeInitializedEvent;

@Component
public class CoreCustomSchemeInitializer implements ApplicationListener<ApplicationStartedEvent> {

    private final CustomSchemeManager schemeManager;

    private final ApplicationEventPublisher eventPublisher;

    public CoreCustomSchemeInitializer(CustomSchemeManager schemeManager,
                                       ApplicationEventPublisher eventPublisher) {
        this.schemeManager = schemeManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationStartedEvent event) {
        // plugin.ikaros.run
        schemeManager.register(Plugin.class);

        // file.ikaros.run
        schemeManager.register(FilePolicy.class);
        schemeManager.register(FileSetting.class);

        eventPublisher.publishEvent(new SchemeInitializedEvent(this));
    }
}
