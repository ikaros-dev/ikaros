package run.ikaros.server.core.subject;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.infra.constant.OpenApiConst;

@Slf4j
// @Component
public class SubjectEndpoint implements CoreEndpoint {
    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/Subject";
        return SpringdocRouteBuilder.route()
            .build();
    }
}
