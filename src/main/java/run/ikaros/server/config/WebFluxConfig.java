package run.ikaros.server.config;

import static org.springframework.util.ResourceUtils.FILE_URL_PREFIX;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.resource.EncodedResourceResolver;
import org.springframework.web.reactive.resource.PathResourceResolver;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;
import org.springframework.web.reactive.result.view.ViewResolver;
import reactor.core.publisher.Mono;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.endpoint.CoreEndpointsBuilder;
import run.ikaros.server.infra.properties.IkarosProperties;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {


    private final ApplicationContext applicationContext;
    private final IkarosProperties ikarosProperties;

    public WebFluxConfig(ApplicationContext applicationContext, IkarosProperties ikarosProperties) {
        this.applicationContext = applicationContext;
        this.ikarosProperties = ikarosProperties;
    }

    @Bean
    ServerResponse.Context context(CodecConfigurer codec,
                                   ViewResolutionResultHandler resultHandler) {
        return new ServerResponse.Context() {
            @Override
            @NonNull
            public List<HttpMessageWriter<?>> messageWriters() {
                return codec.getWriters();
            }

            @Override
            @NonNull
            public List<ViewResolver> viewResolvers() {
                return resultHandler.getViewResolvers();
            }
        };
    }


    @Bean
    RouterFunction<ServerResponse> consoleIndexRedirection() {
        return route(GET("/console")
                .or(GET("/console/index"))
                .or(GET("/console/index.html")),
            request -> ServerResponse.permanentRedirect(URI.create("/console/")).build())
            .and(route(GET("/console/"),
                this::serveConsoleIndex
            ));
    }

    @Bean
    RouterFunction<ServerResponse> coreEndpoints(ApplicationContext context) {
        var builder = new CoreEndpointsBuilder();
        context.getBeansOfType(CoreEndpoint.class).values().forEach(builder::add);
        return builder.build();
    }

    private Mono<ServerResponse> serveConsoleIndex(ServerRequest request) {
        var indexLocation = ikarosProperties.getConsole().getLocation() + "index.html";
        var indexResource = applicationContext.getResource(indexLocation);
        try {
            return ServerResponse.ok()
                .cacheControl(CacheControl.noStore())
                .body(BodyInserters.fromResource(indexResource));
        } catch (Throwable e) {
            return Mono.error(e);
        }
    }


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        var attachmentsRoot = ikarosProperties.getWorkDir().resolve("attachments");
        var cacheControl = CacheControl.maxAge(Duration.ofDays(365 / 2));

        // Mandatory resource mapping
        var uploadRegistration = registry.addResourceHandler("/upload/**")
            .addResourceLocations(FILE_URL_PREFIX + attachmentsRoot.resolve("upload") + "/")
            .setUseLastModified(true)
            .setCacheControl(cacheControl);

        // For console project
        registry.addResourceHandler("/console/**")
            .addResourceLocations(ikarosProperties.getConsole().getLocation())
            .setCacheControl(cacheControl)
            .setUseLastModified(true)
            .resourceChain(true)
            .addResolver(new EncodedResourceResolver())
            .addResolver(new PathResourceResolver());


    }
}
