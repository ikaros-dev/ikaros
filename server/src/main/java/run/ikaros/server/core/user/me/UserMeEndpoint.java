package run.ikaros.server.core.user.me;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springdoc.webmvc.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.user.VerificationCodeType;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.store.entity.User;
import run.ikaros.server.core.user.UpdateUserRequest;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.security.SecurityContextHolderUtils;
import run.ikaros.server.security.SecurityUser;

import java.util.Optional;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

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
                                        .implementation(SecurityUser.class)))

                .GET("/user/me/email/exists", this::existEmail,
                        builder -> builder.operationId("ExistEmail")
                                .tag(tag)
                                .description("Exist email.")
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

    private ServerResponse getUserMe(ServerRequest request) {
        SecurityUser securityUser = SecurityContextHolderUtils.getCurrentSecurityUser();
        if (securityUser != null) {
            securityUser.eraseCredentials();
        }
        return ServerResponse.accepted().body(securityUser);
    }

    private ServerResponse existEmail(ServerRequest request) {
        SecurityUser securityUser = SecurityContextHolderUtils.getCurrentSecurityUser();
        boolean exists = false;
        if (securityUser != null && securityUser.getUser() != null
        && StringUtils.isNotBlank(securityUser.getUser().getEmail())) {
            exists = true;
        }
        return ServerResponse.accepted().body(exists);
    }

    private ServerResponse putProfile(ServerRequest request) {
        throw new UnsupportedOperationException("Not Impl.");
    }

    private ServerResponse updateUsername(ServerRequest request) {
        String newUsername = request.pathVariable("newUsername");
        throw new UnsupportedOperationException("Not Impl.");
    }

    private ServerResponse changeUserPassword(ServerRequest request) {
        Optional<String> oldPassword = request.param("oldPassword");
        Optional<String> newPassword = request.param("newPassword");
        Assert.isTrue(oldPassword.isPresent(), "'oldPassword' must not blank.");
        Assert.isTrue(newPassword.isPresent(), "'newPassword' must not blank.");
        throw new UnsupportedOperationException("Not Impl.");
    }

    private ServerResponse bindEmail(ServerRequest request) {
        Optional<String> emailOptional = request.param("email");
        Optional<String> verificationCodeOptional = request.param("verificationCode");
        Assert.isTrue(emailOptional.isPresent(), "'email' must not blank.");
        Assert.isTrue(verificationCodeOptional.isPresent(), "'verificationCode' must not blank.");
        throw new UnsupportedOperationException("Not Impl.");
    }

    private ServerResponse bindTelephone(ServerRequest request) {
        Optional<String> telephoneOp = request.param("telephone");
        Optional<String> verificationCodeOptional = request.param("verificationCode");
        Assert.isTrue(telephoneOp.isPresent(), "'telephone' must not blank.");
        Assert.isTrue(verificationCodeOptional.isPresent(), "'verificationCode' must not blank.");
        throw new UnsupportedOperationException("Not Impl.");
    }

    private ServerResponse sendVerificationCode(ServerRequest request) {
        VerificationCodeType type = VerificationCodeType.valueOf(request.pathVariable("type"));
        throw new UnsupportedOperationException("Not Impl.");
    }

}