package run.ikaros.server.core.statics;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.apiresponse.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class StaticEndpoint implements CoreEndpoint {
    private final StaticService staticService;

    public StaticEndpoint(StaticService staticService) {
        this.staticService = staticService;
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
        return staticService.listStaticsFonts()
            .collectList()
            .flatMap(fonts -> ServerResponse.ok().bodyValue(fonts));
    }
}
