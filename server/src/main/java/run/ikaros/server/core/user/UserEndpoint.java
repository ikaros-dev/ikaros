package run.ikaros.server.core.user;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
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
            .GET("/user/current", this::getCurrentUserDetail,
                builder -> builder.operationId("GetCurrentUserDetail")
                    .tag(tag)
                    .description("Get current user detail.")
                    .response(responseBuilder()
                        .implementation(User.class)))

            .PUT("/user", this::putUser,
                builder -> builder.operationId("UpdateUser")
                    .tag(tag)
                    .description("Update user information.")
                    .requestBody(requestBodyBuilder()
                        .required(true).implementation(UpdateUserRequest.class)
                        .description("User update info."))
                    .response(org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder()
                        .responseCode("200")
                        .description("Update user information success.")
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
                    .response(org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder()
                        .responseCode("200")
                        .description("Change user role success.")
                        .implementation(Void.class)))

            .PUT("/user/{username}/password", this::changeUserPassword,
                builder -> builder.operationId("ChangeUserPassword")
                    .tag(tag).description("Change user password.")
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.PATH)
                        .name("username").implementation(String.class)
                        .required(true).description("Username for user."))
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("oldPassword").implementation(String.class)
                        .required(true).description("Old password for user."))
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("newPassword").implementation(String.class)
                        .required(true).description("New password for user."))
                    .response(org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder()
                        .responseCode("200")
                        .description("Update user password success.")
                        .implementation(Void.class)))

            .PUT("/user/{username}/email", this::bindEmail,
                builder -> builder.operationId("BindEmail")
                    .deprecated(true)
                    .tag(tag).description("Bind user and email.")
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.PATH)
                        .name("username").implementation(String.class)
                        .required(true).description("Username for user."))
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("email").implementation(String.class)
                        .required(true).description("Email for user."))
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("verificationCode").implementation(String.class)
                        .required(true).description("Verification code once."))
                    .response(org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder()
                        .responseCode("200")
                        .description("Bind user and email success.")
                        .implementation(Void.class)))

            .PUT("/user/{username}/telephone", this::bindTelephone,
                builder -> builder.operationId("BindTelephone")
                    .deprecated(true)
                    .tag(tag).description("Bind user and telephone.")
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.PATH)
                        .name("username").implementation(String.class)
                        .required(true).description("Username for user."))
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("telephone").implementation(String.class)
                        .required(true).description("Telephone for user."))
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("verificationCode").implementation(String.class)
                        .required(true).description("Verification code once."))
                    .response(org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder()
                        .responseCode("200")
                        .description("Bind user and telephone success.")
                        .implementation(Void.class)))
            .build();
    }

    private Mono<ServerResponse> getCurrentUserDetail(ServerRequest request) {
        return ReactiveSecurityContextHolder.getContext()
            .switchIfEmpty(Mono.error(
                new AuthenticationCredentialsNotFoundException("Not found, please login")))
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getPrincipal)
            .map(principal -> (UserDetails) principal)
            .map(UserDetails::getUsername)
            .flatMap(userService::getUserByUsername)
            .flatMap(user -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user));
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

    private Mono<ServerResponse> changeRole(ServerRequest request) {
        String username = request.pathVariable("username");
        return Mono.justOrEmpty(request.queryParam("roleId"))
            .map(Long::valueOf)
            .flatMap(roleId -> userService.changeRole(username, roleId))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> changeUserPassword(ServerRequest request) {
        String username = request.pathVariable("username");
        Optional<String> oldPassword = request.queryParam("oldPassword");
        Optional<String> newPassword = request.queryParam("newPassword");
        Assert.isTrue(oldPassword.isPresent(), "'oldPassword' must not blank.");
        Assert.isTrue(newPassword.isPresent(), "'newPassword' must not blank.");
        return userService.updatePassword(username, oldPassword.get(), newPassword.get())
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> bindEmail(ServerRequest request) {
        String username = request.pathVariable("username");
        Optional<String> emailOptional = request.queryParam("email");
        Optional<String> verificationCodeOptional = request.queryParam("verificationCode");
        Assert.isTrue(emailOptional.isPresent(), "'email' must not blank.");
        Assert.isTrue(verificationCodeOptional.isPresent(), "'verificationCode' must not blank.");
        return userService.bindEmail(username, emailOptional.get(), verificationCodeOptional.get())
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> bindTelephone(ServerRequest request) {
        String username = request.pathVariable("username");
        Optional<String> telephoneOp = request.queryParam("telephone");
        Optional<String> verificationCodeOptional = request.queryParam("verificationCode");
        Assert.isTrue(telephoneOp.isPresent(), "'telephone' must not blank.");
        Assert.isTrue(verificationCodeOptional.isPresent(), "'verificationCode' must not blank.");
        return userService.bindTelephone(username, telephoneOp.get(),
                verificationCodeOptional.get())
            .then(ServerResponse.ok().build());
    }
}
