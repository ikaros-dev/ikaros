package run.ikaros.server.core.authority;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
//@Component
public class AuthorityEndpoint implements CoreEndpoint {
    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/authority";
        return SpringdocRouteBuilder.route().build();
    }
}
