package run.ikaros.server.core.role;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;

import lombok.extern.slf4j.Slf4j;
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
public class RoleEndpoint implements CoreEndpoint {
    private final RoleService roleService;

    public RoleEndpoint(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/role";
        return SpringdocRouteBuilder.route()
            .GET("/roles", this::getRoles,
                builder -> builder.operationId("GetRoles")
                    .tag(tag).description("Get Roles")
                    .response(responseBuilder()
                        .implementationArray(String.class)))

            .build();
    }

    private Mono<ServerResponse> getRoles(ServerRequest serverRequest) {
        return Mono.empty();
    }


}
