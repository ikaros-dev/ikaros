package run.ikaros.server.core.role;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.role.RoleAuthorityReqParams;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class RoleAuthorityEndpoint implements CoreEndpoint {

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/role/authority";
        return SpringdocRouteBuilder.route()

            .POST("/role/authorities", this::addAuthoritiesForRole,
                builder -> builder.operationId("AddAuthoritiesForRole")
                    .tag(tag).description("Add authorities for role")
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(RoleAuthorityReqParams.class)))

            .DELETE("/role/authorities", this::deleteAuthoritiesForRole,
                builder -> builder.operationId("DeleteAuthoritiesForRole")
                    .tag(tag).description("Delete authorities for role")
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(RoleAuthorityReqParams.class)))

            .build();
    }

    private Mono<ServerResponse> addAuthoritiesForRole(ServerRequest serverRequest) {
        return Mono.empty();
    }

    private Mono<ServerResponse> deleteAuthoritiesForRole(ServerRequest serverRequest) {
        return Mono.empty();
    }


}
