package run.ikaros.server.core.user.me;

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
import run.ikaros.api.core.user.enums.VerificationCodeType;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.server.core.user.UpdateUserRequest;
import run.ikaros.server.core.user.User;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.store.entity.BaseEntity;
import run.ikaros.server.store.entity.UserEntity;

@Slf4j
@Component
public class UserMeEndpoint implements CoreEndpoint {
    private final UserService userService;

    public UserMeEndpoint(UserService userService) {
        this.userService = userService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/user/me";
        return SpringdocRouteBuilder.route()
            .GET("/user/me", this::getUserMe,
                builder -> builder.operationId("GetUserMe")
                    .tag(tag)
                    .description("Get user me.")
                    .response(responseBuilder()
                        .implementation(User.class)))

            .GET("/user/me/email/exists/{email}", this::existEmail,
                builder -> builder.operationId("ExistEmail")
                    .tag(tag)
                    .description("Exist email.")
                    .parameter(Builder.parameterBuilder()
                        .name("email").required(true).in(ParameterIn.PATH))
                    .response(responseBuilder().implementation(Boolean.class)))

            .PUT("/user/me", this::putProfile,
                builder -> builder.operationId("PutProfile")
                    .tag(tag)
                    .description("Update user profile.")
                    .requestBody(requestBodyBuilder()
                        .required(true).implementation(UpdateUserRequest.class)
                        .description("User update info."))
                    .response(responseBuilder()
                        .responseCode("200")
                        .description("Update user information success.")
                        .implementation(User.class)))

            .PUT("/user/me/username/{newUsername}", this::updateUsername,
                builder -> builder.operationId("UpdateUsername")
                    .tag(tag).description("Update user username.")
                    .parameter(Builder.parameterBuilder()
                        .name("newUsername").required(true).in(ParameterIn.PATH)))


            .PUT("/user/me/password", this::changeUserPassword,
                builder -> builder.operationId("ChangeUserPassword")
                    .tag(tag).description("Change user password.")
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("oldPassword").implementation(String.class)
                        .required(true).description("Old password for user."))
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("newPassword").implementation(String.class)
                        .required(true).description("New password for user."))
                    .response(responseBuilder()
                        .responseCode("200")
                        .description("Update user password success.")
                        .implementation(Void.class)))

            .PUT("/user/me/email", this::bindEmail,
                builder -> builder.operationId("BindEmail")
                    .deprecated(true)
                    .tag(tag).description("Bind user and email.")
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("email").implementation(String.class)
                        .required(true).description("Email for user."))
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("verificationCode").implementation(String.class)
                        .required(true).description("Verification code once."))
                    .response(responseBuilder()
                        .responseCode("200")
                        .description("Bind user and email success.")
                        .implementation(Void.class)))

            .PUT("/user/me/telephone", this::bindTelephone,
                builder -> builder.operationId("BindTelephone")
                    .deprecated(true)
                    .tag(tag).description("Bind user and telephone.")
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("telephone").implementation(String.class)
                        .required(true).description("Telephone for user."))
                    .parameter(Builder.parameterBuilder()
                        .in(ParameterIn.DEFAULT)
                        .name("verificationCode").implementation(String.class)
                        .required(true).description("Verification code once."))
                    .response(responseBuilder()
                        .responseCode("200")
                        .description("Bind user and telephone success.")
                        .implementation(Void.class)))

            .PUT("/user/me/verificationCode/{type}", this::sendVerificationCode,
                builder -> builder.operationId("SendVerificationCode")
                    .tag(tag).description("Send verification code.")
                    .parameter(Builder.parameterBuilder()
                        .name("type").required(true).in(ParameterIn.PATH)
                        .implementation(VerificationCodeType.class)))

            .build();
    }

    private Mono<ServerResponse> getUserMe(ServerRequest request) {
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

    private Mono<ServerResponse> existEmail(ServerRequest request) {
        return Mono.just(request)
            .map(req -> req.pathVariable("email"))
            .flatMap(userService::existsByEmail)
            .flatMap(exists -> ServerResponse.ok().bodyValue(exists));
    }

    private Mono<ServerResponse> putProfile(ServerRequest request) {
        return request.bodyToMono(UpdateUserRequest.class)
            .map(user -> {
                user.setEnable(null);
                return user;
            })
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

    private Mono<ServerResponse> updateUsername(ServerRequest request) {
        String newUsername = request.pathVariable("newUsername");
        return userService.getUserFromSecurityContext()
            .map(User::entity)
            .map(UserEntity::getUsername)
            .flatMap(userService::getUserByUsername)
            .map(User::entity)
            .map(BaseEntity::getId)
            .flatMap(userId -> userService.updateUsername(userId, newUsername))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> changeUserPassword(ServerRequest request) {
        Optional<String> oldPassword = request.queryParam("oldPassword");
        Optional<String> newPassword = request.queryParam("newPassword");
        Assert.isTrue(oldPassword.isPresent(), "'oldPassword' must not blank.");
        Assert.isTrue(newPassword.isPresent(), "'newPassword' must not blank.");
        return userService.getUserFromSecurityContext()
            .map(User::entity)
            .map(UserEntity::getUsername)
            .flatMap(username ->
                userService.updatePassword(username, oldPassword.get(), newPassword.get()))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> bindEmail(ServerRequest request) {
        Optional<String> emailOptional = request.queryParam("email");
        Optional<String> verificationCodeOptional = request.queryParam("verificationCode");
        Assert.isTrue(emailOptional.isPresent(), "'email' must not blank.");
        Assert.isTrue(verificationCodeOptional.isPresent(), "'verificationCode' must not blank.");
        return userService.getUserFromSecurityContext()
            .map(User::entity)
            .map(UserEntity::getUsername)
            .flatMap(username ->
                userService.bindEmail(username,
                    emailOptional.get(), verificationCodeOptional.get()))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> bindTelephone(ServerRequest request) {
        Optional<String> telephoneOp = request.queryParam("telephone");
        Optional<String> verificationCodeOptional = request.queryParam("verificationCode");
        Assert.isTrue(telephoneOp.isPresent(), "'telephone' must not blank.");
        Assert.isTrue(verificationCodeOptional.isPresent(), "'verificationCode' must not blank.");
        return userService.getUserFromSecurityContext()
            .map(User::entity)
            .map(UserEntity::getUsername)
            .flatMap(username -> userService.bindTelephone(username, telephoneOp.get(),
                verificationCodeOptional.get()))
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> sendVerificationCode(ServerRequest request) {
        VerificationCodeType type = VerificationCodeType.valueOf(request.pathVariable("type"));
        return userService.getUserFromSecurityContext()
            .map(User::entity)
            .map(UserEntity::getUsername)
            .flatMap(userService::getUserByUsername)
            .map(User::entity)
            .map(BaseEntity::getId)
            .flatMap(userId -> userService.sendVerificationCode(userId, type))
            .then(ServerResponse.ok().build());
    }

}
