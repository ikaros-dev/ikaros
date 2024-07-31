package run.ikaros.server.core.user;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class UserEndpoint implements CoreEndpoint {
    private final UserService userService;

    public UserEndpoint(UserService userService) {
        this.userService = userService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/user";
        return SpringdocRouteBuilder.route()
            .GET("/users", this::getUsers,
                builder -> builder.operationId("GetUsers")
                    .tag(tag).description("Get all users.")
                    .response(responseBuilder()
                        .implementationArray(User.class)))

            .GET("/user/username/exists/{username}", this::existUserByUsername,
                builder -> builder.operationId("ExistUserByUsername")
                    .tag(tag)
                    .description("Exist user by username.")
                    .parameter(Builder.parameterBuilder()
                        .name("username").required(true).in(ParameterIn.PATH))
                    .response(responseBuilder().implementation(Boolean.class)))

            .GET("/user/email/exists/{email}", this::existUserByEmail,
                builder -> builder.operationId("ExistUserByEmail")
                    .tag(tag)
                    .description("Exist user by email.")
                    .parameter(Builder.parameterBuilder()
                        .name("email").required(true).in(ParameterIn.PATH))
                    .response(responseBuilder().implementation(Boolean.class)))

            .PUT("/user", this::putUser,
                builder -> builder.operationId("UpdateUser")
                    .tag(tag)
                    .description("Update user information.")
                    .requestBody(requestBodyBuilder()
                        .required(true).implementation(UpdateUserRequest.class)
                        .description("User update info."))
                    .response(responseBuilder()
                        .responseCode("200")
                        .description("Update user information success.")
                        .implementation(User.class)))

            .POST("/user", this::postUser,
                builder -> builder.operationId("PostUser")
                    .tag(tag)
                    .description("Create user.")
                    .requestBody(requestBodyBuilder()
                        .required(true).implementation(CreateUserReqParams.class)
                        .description("User info."))
                    .response(responseBuilder()
                        .responseCode("200")
                        .description("Create user information success.")
                        .implementation(User.class)))

            .PUT("/user/{username}/role", this::changeRole,
                builder -> builder.operationId("ChangeUserRole")
                    .tag(tag).description("Change user role by username and roleId.")
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.PATH)
                        .name("username").implementation(String.class)
                        .required(true).description("Username for user."))
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("roleId").implementation(Long.class)
                        .required(true).description("Id for role."))
                    .response(responseBuilder()
                        .responseCode("200")
                        .description("Change user role success.")
                        .implementation(Void.class)))


            .DELETE("/user/id/{id}", this::deleteById,
                builder -> builder.operationId("DeleteById")
                    .tag(tag).description("Delete user by id..")
                    .parameter(Builder.parameterBuilder()
                        .name("id").required(true).in(ParameterIn.PATH)))
            .build();
    }

    private Mono<ServerResponse> getUsers(ServerRequest request) {
        return userService.findAll()
            .collectList()
            .flatMap(users -> ServerResponse.ok().bodyValue(users));
    }

    private Mono<ServerResponse> existUserByUsername(ServerRequest request) {
        return Mono.just(request)
            .map(req -> req.pathVariable("username"))
            .flatMap(userService::existsByUsername)
            .flatMap(exists -> ServerResponse.ok().bodyValue(exists));
    }

    private Mono<ServerResponse> existUserByEmail(ServerRequest request) {
        return Mono.just(request)
            .map(req -> req.pathVariable("email"))
            .flatMap(userService::existsByEmail)
            .flatMap(exists -> ServerResponse.ok().bodyValue(exists));
    }

    private Mono<ServerResponse> putUser(ServerRequest request) {
        return request.bodyToMono(UpdateUserRequest.class)
            .flatMap(userService::update)
            .flatMap(user -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user))
            .onErrorResume(NotFoundException.class,
                e -> ServerResponse.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(e.getMessage()))
            .onErrorResume(IllegalArgumentException.class,
                e -> ServerResponse.badRequest()
                    .bodyValue("No user id. exception msg:" + e.getMessage()));
    }

    private Mono<ServerResponse> postUser(ServerRequest request) {
        return request.bodyToMono(CreateUserReqParams.class)
            .flatMap(userService::create)
            .flatMap(user -> ServerResponse.ok().bodyValue(user));
    }

    private Mono<ServerResponse> changeRole(ServerRequest request) {
        String username = request.pathVariable("username");
        return Mono.justOrEmpty(request.queryParam("roleId"))
            .map(Long::valueOf)
            .flatMap(roleId -> userService.changeRole(username, roleId))
            .then(ServerResponse.ok().build());
    }


    private Mono<ServerResponse> deleteById(ServerRequest request) {
        Long userId = Long.valueOf(request.pathVariable("id"));
        return userService.deleteById(userId)
            .then(ServerResponse.ok().build());
    }
}
