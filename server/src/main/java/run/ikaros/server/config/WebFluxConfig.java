package run.ikaros.server.config;

import static java.io.File.separatorChar;
import static org.springframework.util.ResourceUtils.FILE_URL_PREFIX;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static run.ikaros.api.constant.AppConst.STATIC_DIR_NAME;
import static run.ikaros.api.core.attachment.AttachmentConst.DRIVER_STATIC_RESOURCE_PREFIX;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.HttpMessageWriter;
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
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.constant.FileConst;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.server.console.ConsoleProperties;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.endpoint.CoreEndpointsBuilder;
import run.ikaros.server.plugin.PluginApplicationContextRegistry;

@Configuration(proxyBeanMethods = false)
public class WebFluxConfig implements WebFluxConfigurer {
    private final DynamicDirectoryResolver dynamicDirectoryResolver;

    private final ApplicationContext applicationContext;
    private final IkarosProperties ikarosProperties;
    private final ConsoleProperties consoleProperties;

    /**
     * construct a {@link WebFluxConfig} instance.
     *
     * @param applicationContext root application context
     * @param ikarosProperties   ikaros prop
     * @param consoleProperties  console prop
     */
    public WebFluxConfig(
        DynamicDirectoryResolver dynamicDirectoryResolver, ApplicationContext applicationContext,
        IkarosProperties ikarosProperties,
        ConsoleProperties consoleProperties) {
        this.dynamicDirectoryResolver = dynamicDirectoryResolver;
        this.applicationContext = applicationContext;
        this.ikarosProperties = ikarosProperties;
        this.consoleProperties = consoleProperties;
    }

    @Bean
    ServerResponse.Context context(CodecConfigurer codec,
                                   ViewResolutionResultHandler resultHandler) {
        return new ServerResponse.Context() {
            @Override
            public @NonNull List<HttpMessageWriter<?>> messageWriters() {
                return codec.getWriters();
            }

            @Override
            public @NonNull List<ViewResolver> viewResolvers() {
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
        PluginApplicationContextRegistry.getInstance().getPluginApplicationContexts()
            .forEach(pluginApplicationContext ->
                pluginApplicationContext.getBeansOfType(CoreEndpoint.class).values()
                    .forEach(builder::add));
        return builder.build();
    }

    private Mono<ServerResponse> serveConsoleIndex(ServerRequest request) {
        var indexLocation = consoleProperties.getLocation() + "index.html";
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
        var importRoot = ikarosProperties.getWorkDir().resolve(FileConst.DEFAULT_DIR_NAME);
        var cacheControl = CacheControl.maxAge(Duration.ofDays(365 / 2));

        // Mandatory resource mapping
        registry.addResourceHandler("/" + FileConst.DEFAULT_DIR_NAME + "/**")
            .addResourceLocations(FILE_URL_PREFIX + importRoot + "/")
            .setUseLastModified(true)
            .setCacheControl(cacheControl);

        // For console project
        registry.addResourceHandler("/console/**")
            .addResourceLocations(consoleProperties.getLocation())
            .setCacheControl(cacheControl)
            .setUseLastModified(true)
            .resourceChain(true)
            .addResolver(new EncodedResourceResolver())
            .addResolver(new PathResourceResolver());

        // Add user and thymeleaf static resource
        registry.addResourceHandler("/static/**")
            .addResourceLocations(
                "file:" + ikarosProperties.getWorkDir().resolve(STATIC_DIR_NAME)
                    + separatorChar,
                "classpath:/static/",
                "classpath:/templates/static/")
            .setCacheControl(cacheControl)
            .setUseLastModified(true);

        // /theme/simple/static => classpath:/templates/theme/simple/static/
        // Register classpath default theme static file mapping
        registry.addResourceHandler("/theme/simple/static/**")
            .addResourceLocations("classpath:/templates/simple/static/")
            .setCacheControl(cacheControl)
            .setUseLastModified(true);
        // Register user themes dir all theme static file mapping
        Path themesDirPath = ikarosProperties.getWorkDir().resolve(AppConst.THEME_DIR_NAME);
        if (Files.notExists(themesDirPath)) {
            try {
                Files.createDirectory(themesDirPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        File[] themeDirs = themesDirPath.toFile().listFiles();
        if (Objects.nonNull(themeDirs)) {
            for (File themeDir : themeDirs) {
                if (themeDir.isFile()) {
                    continue;
                }
                registry.addResourceHandler("/theme/" + themeDir.getName() + "/static/**")
                    .addResourceLocations(
                        "file:" + themeDir + separatorChar + "static" + separatorChar)
                    .setCacheControl(cacheControl)
                    .setUseLastModified(true);
            }
        }

        // add dynamic resource resolver
        registry.addResourceHandler(DRIVER_STATIC_RESOURCE_PREFIX + "/**")
            .setCacheControl(cacheControl)
            .setUseLastModified(true)
            .resourceChain(true)
            .addResolver(dynamicDirectoryResolver)
        ;
    }
}
