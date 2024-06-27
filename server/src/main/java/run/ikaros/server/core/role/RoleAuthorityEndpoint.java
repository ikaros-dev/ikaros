package run.ikaros.server.core.role;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.authority.Authority;
import run.ikaros.api.core.role.RoleAuthorityReqParams;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class RoleAuthorityEndpoint implements CoreEndpoint {
    private final RoleAuthorityService roleAuthorityService;

    public RoleAuthorityEndpoint(RoleAuthorityService roleAuthorityService) {
        this.roleAuthorityService = roleAuthorityService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/role/authority";
        return SpringdocRouteBuilder.route()

            .POST("/role/authorities", this::addAuthoritiesForRole,
                builder -> builder.operationId("AddAuthoritiesForRole")
                    .tag(tag).description("Add authorities for role")
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(RoleAuthorityReqParams.class))
                    .response(responseBuilder()
                        .implementationArray(Authority.class)))

            .DELETE("/role/authorities", this::deleteAuthoritiesForRole,
                builder -> builder.operationId("DeleteAuthoritiesForRole")
                    .tag(tag).description("Delete authorities for role")
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(RoleAuthorityReqParams.class)))

            .GET("/role/authorities/roleId/{roleId}", this::getAuthoritiesForRole,
                builder -> builder.operationId("GetAuthoritiesForRole")
                    .tag(tag).description("Get authorities for role")
                    .response(responseBuilder()
                        .implementationArray(Authority.class)))

            .build();
    }

    private Mono<ServerResponse> addAuthoritiesForRole(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(RoleAuthorityReqParams.class)
            .flatMapMany(roleAuthorityReqParams ->
                roleAuthorityService.addAuthoritiesForRole(
                    roleAuthorityReqParams.getRoleId(),
                    roleAuthorityReqParams.getAuthorityIds()
                ))
            .collectList()
            .flatMap(authorities -> ServerResponse.ok().bodyValue(authorities));
    }

    private Mono<ServerResponse> deleteAuthoritiesForRole(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(RoleAuthorityReqParams.class)
            .flatMapMany(roleAuthorityReqParams ->
                roleAuthorityService.deleteAuthoritiesForRole(
                    roleAuthorityReqParams.getRoleId(),
                    roleAuthorityReqParams.getAuthorityIds()
                ))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> getAuthoritiesForRole(ServerRequest serverRequest) {
        Long roleId = Long.valueOf(serverRequest.pathVariable("roleId"));
        return roleAuthorityService.getAuthoritiesForRole(roleId)
            .collectList()
            .flatMap(authorities -> ServerResponse.ok().bodyValue(authorities));
    }


}
