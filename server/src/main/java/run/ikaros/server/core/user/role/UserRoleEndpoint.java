package run.ikaros.server.core.user.role;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.role.Role;
import run.ikaros.api.core.user.UserRoleReqParams;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class UserRoleEndpoint implements CoreEndpoint {
    private final UserRoleService userRoleService;

    public UserRoleEndpoint(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/user/role";
        return SpringdocRouteBuilder.route()

            .POST("/user/roles", this::addUserRoles,
                builder -> builder.operationId("AddUserRoles")
                    .tag(tag).description("Add user roles")
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(UserRoleReqParams.class))
                    .response(responseBuilder()
                        .implementationArray(Role.class)))

            .DELETE("/user/role", this::deleteUserRoles,
                builder -> builder.operationId("DeleteUserRoles")
                    .tag(tag).description("Delete user roles")
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(UserRoleReqParams.class)))

            .GET("/user/role/userId/{userId}", this::getRolesForUser,
                builder -> builder.operationId("getRolesForUser")
                    .tag(tag).description("Get roles for user")
                    .parameter(parameterBuilder()
                        .name("userId").required(true).in(ParameterIn.PATH))
                    .response(responseBuilder()
                        .implementationArray(Role.class)))

            .build();
    }

    private Mono<ServerResponse> addUserRoles(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(UserRoleReqParams.class)
            .flatMapMany(userRoleReqParams ->
                userRoleService.addUserRoles(
                    userRoleReqParams.getUserId(),
                    userRoleReqParams.getRoleIds()
                ))
            .collectList()
            .flatMap(authorities -> ServerResponse.ok().bodyValue(authorities));
    }

    private Mono<ServerResponse> deleteUserRoles(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(UserRoleReqParams.class)
            .flatMapMany(userRoleReqParams ->
                userRoleService.deleteUserRoles(
                    userRoleReqParams.getUserId(),
                    userRoleReqParams.getRoleIds()
                ))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> getRolesForUser(ServerRequest serverRequest) {
        UUID userId = UuidV7Utils.fromString(serverRequest.pathVariable("userId"));
        return userRoleService.getRolesForUser(userId)
            .collectList()
            .flatMap(roles -> ServerResponse.ok().bodyValue(roles));
    }


}
