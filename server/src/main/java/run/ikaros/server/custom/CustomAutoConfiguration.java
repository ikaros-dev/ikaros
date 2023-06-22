package run.ikaros.server.custom;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.custom.scheme.CustomSchemeManager;
import run.ikaros.server.custom.router.CustomCompositeRouterFunction;
import run.ikaros.server.custom.scheme.CustomSchemeWatcherManager;
import run.ikaros.server.custom.scheme.DefaultCustomSchemeManager;
import run.ikaros.server.custom.scheme.DefaultCustomSchemeWatcherManager;

@Configuration(proxyBeanMethods = false)
public class CustomAutoConfiguration {

    @Bean
    CustomSchemeWatcherManager schemeWatcherManager() {
        return new DefaultCustomSchemeWatcherManager();
    }

    @Bean
    CustomCompositeRouterFunction extensionsRouterFunction(
        ReactiveCustomClient client,
        CustomSchemeWatcherManager schemeWatcherManager,
        ApplicationEventPublisher applicationEventPublisher) {
        return new CustomCompositeRouterFunction(client, schemeWatcherManager,
            applicationEventPublisher);
    }

    @Bean
    CustomSchemeManager schemeManager(CustomSchemeWatcherManager watcherManager) {
        return new DefaultCustomSchemeManager(watcherManager);
    }

}
