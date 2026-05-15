package run.ikaros.server.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.endpoint.CoreEndpointsBuilder;

@Configuration(proxyBeanMethods = false)
public class WebConfiguration {
    @Bean
    RouterFunction<ServerResponse> coreEndpoints(ApplicationContext context) {
        var builder = new CoreEndpointsBuilder();
        context.getBeansOfType(CoreEndpoint.class).values().forEach(builder::add);
        // PluginApplicationContextRegistry.getInstance().getPluginApplicationContexts()
        //         .forEach(pluginApplicationContext ->
        //                 pluginApplicationContext.getBeansOfType(CoreEndpoint.class).values()
        //                         .forEach(builder::add));
        return builder.build();
    }
}
