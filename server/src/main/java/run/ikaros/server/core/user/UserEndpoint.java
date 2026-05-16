package run.ikaros.server.core.user;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springdoc.webmvc.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.store.entity.User;
import run.ikaros.server.endpoint.CoreEndpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

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

    private ServerResponse getUsers(ServerRequest request) {
        throw new UnsupportedOperationException("Not Impl.");
    }

    private ServerResponse existUserByUsername(ServerRequest request) {
        throw new UnsupportedOperationException("Not Impl.");
    }

    private ServerResponse existUserByEmail(ServerRequest request) {
        throw new UnsupportedOperationException("Not Impl.");
    }

    private ServerResponse putUser(ServerRequest request) {
        throw new UnsupportedOperationException("Not Impl.");
    }

    private ServerResponse postUser(ServerRequest request) {
        throw new UnsupportedOperationException("Not Impl.");
    }

    private ServerResponse changeRole(ServerRequest request) {
        throw new UnsupportedOperationException("Not Impl.");
    }


    private ServerResponse deleteById(ServerRequest request) {
        throw new UnsupportedOperationException("Not Impl.");
    }
}