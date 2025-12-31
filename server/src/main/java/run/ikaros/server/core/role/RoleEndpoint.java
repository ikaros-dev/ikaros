package run.ikaros.server.core.role;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.role.Role;
import run.ikaros.api.infra.utils.UuidV7Utils;
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
                        .implementationArray(Role.class)))

            .POST("/role", this::postRole,
                builder -> builder.operationId("CreateRole")
                    .tag(tag).description("Create Role")
                    .requestBody(requestBodyBuilder()
                        .implementation(Role.class)
                        .required(true)
                        .description("Role"))
                    .response(responseBuilder()
                        .implementationArray(Role.class)))

            .PUT("/role", this::putRole,
                builder -> builder.operationId("UpdateRole")
                    .tag(tag).description("Update Role")
                    .requestBody(requestBodyBuilder()
                        .implementation(Role.class)
                        .required(true)
                        .description("Role"))
                    .response(responseBuilder()
                        .implementationArray(Role.class)))

            .DELETE("/role/id/{id}", this::deleteRoleById,
                builder -> builder.operationId("DeleteRoleById")
                    .tag(tag).description("Delete Role By Id")
                    .parameter(parameterBuilder()
                        .in(ParameterIn.PATH)
                        .required(true)
                        .name("id")
                        .description("Role id")))

            .build();
    }

    private Mono<ServerResponse> getRoles(ServerRequest serverRequest) {
        return roleService.findAll()
            .collectList()
            .flatMap(roles -> ServerResponse.ok().bodyValue(roles));
    }

    private Mono<ServerResponse> postRole(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(Role.class)
            .flatMap(roleService::save)
            .flatMap(role -> ServerResponse.ok().bodyValue(role));
    }

    private Mono<ServerResponse> putRole(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(Role.class)
            .flatMap(roleService::save)
            .flatMap(role -> ServerResponse.ok().bodyValue(role));
    }

    private Mono<ServerResponse> deleteRoleById(ServerRequest serverRequest) {
        final UUID roleId = UuidV7Utils.fromString(serverRequest.pathVariable("id"));
        return roleService.deleteById(roleId)
            .then(ServerResponse.ok().build());
    }


}
