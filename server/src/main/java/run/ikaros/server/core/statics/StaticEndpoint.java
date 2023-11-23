package run.ikaros.server.core.statics;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.apiresponse.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class StaticEndpoint implements CoreEndpoint {
    private final IkarosProperties ikarosProperties;

    public StaticEndpoint(IkarosProperties ikarosProperties) {
        this.ikarosProperties = ikarosProperties;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/static";
        return SpringdocRouteBuilder.route()
            .GET("/static/fonts", this::listStaticsFonts,
                builder -> builder.operationId("ListStaticsFonts")
                    .tag(tag).description("List font dir all fonts in work statics dir.")
                    .response(Builder.responseBuilder().implementationArray(String.class)))
            .build();
    }

    private Mono<ServerResponse> listStaticsFonts(ServerRequest request) {
        final String fontBaseUrl = "/static/" + AppConst.STATIC_FONT_DIR_NAME + '/';
        Path staticsDirPath = ikarosProperties.getWorkDir().resolve(AppConst.STATIC_DIR_NAME);
        if (Files.notExists(staticsDirPath)) {
            try {
                Files.createDirectory(staticsDirPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return ServerResponse.notFound().build();
        }
        Path staticsFontsDirPath = staticsDirPath.resolve(AppConst.STATIC_FONT_DIR_NAME);
        if (Files.notExists(staticsFontsDirPath)) {
            try {
                Files.createDirectory(staticsFontsDirPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return ServerResponse.notFound().build();
        }

        File[] files = staticsFontsDirPath.toFile().listFiles();

        if (Objects.isNull(files)) {
            return ServerResponse.notFound().build();
        }

        List<String> fontUrls = Arrays.stream(files)
            .map(File::getName)
            .map(name -> fontBaseUrl + name)
            .toList();
        return ServerResponse.ok().bodyValue(fontUrls);
    }
}
