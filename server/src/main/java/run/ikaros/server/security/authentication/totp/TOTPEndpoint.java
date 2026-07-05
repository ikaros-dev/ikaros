package run.ikaros.server.security.authentication.totp;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.parameter.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.exception.security.InvalidTokenException;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.security.authentication.jwt.JwtAuthenticationProvider;
import run.ikaros.server.store.entity.UserTotpEntity;
import run.ikaros.server.store.repository.UserRepository;
import run.ikaros.server.store.repository.UserTotpRepository;

@Slf4j
@Component
public class TOTPEndpoint implements CoreEndpoint {
    private final TOTPService totpService;
    private final UserTotpRepository userTotpRepository;
    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final ReactiveUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public TOTPEndpoint(TOTPService totpService,
                        UserTotpRepository userTotpRepository,
                        JwtAuthenticationProvider jwtAuthenticationProvider,
                        ReactiveUserDetailsService userDetailsService,
                        UserRepository userRepository) {
        this.totpService = totpService;
        this.userTotpRepository = userTotpRepository;
        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/security/auth/totp";
        return SpringdocRouteBuilder.route()
            // Step 2: 验证TOTP并获取正式JWT
            .POST("/security/auth/totp/validate", this::validate,
                builder -> builder.operationId("ValidateTotp")
                    .tag(tag)
                    .description("验证TOTP验证码，返回正式JWT令牌。"
                        + "第一步调用applyJwtToken返回totpRequired=true后再调用此接口。")
                    .requestBody(requestBodyBuilder()
                        .required(true).implementation(TotpValidateParam.class))
                    .response(responseBuilder()
                        .implementation(run.ikaros.server.security.authentication.jwt
                            .JwtApplyResponse.class)))
            // 设置TOTP：生成密钥
            .POST("/security/auth/totp/setup", this::setup,
                builder -> builder.operationId("SetupTotp")
                    .tag(tag)
                    .description("生成新的TOTP密钥和otpauth URI（需要先登录）")
                    .response(responseBuilder()
                        .implementation(TotpSetupResponse.class)))
            // 验证并启用TOTP
            .POST("/security/auth/totp/enable", this::enable,
                builder -> builder.operationId("EnableTotp")
                    .tag(tag)
                    .description("验证TOTP验证码并启用二步验证（需要先登录）")
                    .parameter(Builder.parameterBuilder()
                        .name("code").in(io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY)
                        .description("TOTP验证码(6位)").implementation(String.class))
                    .response(responseBuilder()
                        .implementation(String.class)))
            // 禁用TOTP
            .POST("/security/auth/totp/disable", this::disable,
                builder -> builder.operationId("DisableTotp")
                    .tag(tag)
                    .description("禁用二步验证（需要先登录，需验证当前密码）")
                    .parameter(Builder.parameterBuilder()
                        .name("password")
                        .in(io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY)
                        .description("当前登录密码").implementation(String.class))
                    .response(responseBuilder()
                        .implementation(String.class)))
            // 查询TOTP状态
            .GET("/security/auth/totp/status", this::status,
                builder -> builder.operationId("StatusTotp")
                    .tag(tag)
                    .description("查询当前用户的TOTP二步验证状态")
                    .response(responseBuilder()
                        .implementation(TotpStatusResponse.class)))
            .build();
    }

    /**
     * 第二步：验证TOTP并发放正式JWT.
     */
    private Mono<ServerResponse> validate(ServerRequest request) {
        return request.bodyToMono(TotpValidateParam.class)
            .flatMap(param -> {
                String tempToken = param.getTempToken();
                String code = param.getCode();
                if (tempToken == null || code == null) {
                    return Mono.error(new IllegalArgumentException(
                        "tempToken and code are required"));
                }
                // 验证tempToken并获取用户名
                String username;
                try {
                    username = jwtAuthenticationProvider.tempTokenUsername(tempToken);
                } catch (InvalidTokenException e) {
                    return Mono.error(e);
                }
                String finalUsername = username;
                // 查询用户TOTP配置
                return userRepository.findByUsernameAndEnableAndDeleteStatus(username, true, false)
                    .switchIfEmpty(Mono.error(new NotFoundException(
                        "User not found: " + username)))
                    .flatMap(userEntity ->
                        userTotpRepository.findByUserId(userEntity.getId()))
                    .switchIfEmpty(Mono.error(new InvalidTokenException(
                        "TOTP not configured")))
                    .filter(UserTotpEntity::getEnabled)
                    .switchIfEmpty(Mono.error(new InvalidTokenException(
                        "TOTP not enabled")))
                    .filter(totpEntity -> totpService.validateCode(
                        totpEntity.getSecret(), code))
                    .switchIfEmpty(Mono.error(new InvalidTokenException(
                        "Invalid TOTP code")))
                    .flatMap(totpEntity ->
                        userDetailsService.findByUsername(finalUsername))
                    .flatMap(jwtAuthenticationProvider::generateJwtResp)
                    .flatMap(resp -> ServerResponse.ok().bodyValue(resp));
            });
    }

    /**
     * 生成TOTP密钥（需要已登录）.
     */
    private Mono<ServerResponse> setup(ServerRequest request) {
        return getCurrentUsername()
            .flatMap(username -> {
                String secret = totpService.generateSecret();
                String otpAuthUri = totpService.generateOtpAuthUri(username, secret);
                // 持久化密钥
                return getCurrentUserId()
                    .flatMap(userId -> userTotpRepository.findByUserId(userId)
                        .defaultIfEmpty(new UserTotpEntity())
                        .flatMap(existing -> {
                            existing.setUserId(userId);
                            existing.setSecret(secret);
                            existing.setEnabled(false);
                            if (existing.getCreateTime() == null) {
                                existing.setCreateTime(java.time.LocalDateTime.now());
                            }
                            existing.setUpdateTime(java.time.LocalDateTime.now());
                            return userTotpRepository.save(existing);
                        }))
                    .thenReturn(TotpSetupResponse.builder()
                        .secret(secret)
                        .otpAuthUri(otpAuthUri)
                        .build());
            })
            .flatMap(resp -> ServerResponse.ok().bodyValue(resp));
    }

    /**
     * 验证并启用TOTP.
     */
    private Mono<ServerResponse> enable(ServerRequest request) {
        String code = request.queryParam("code").orElse(null);
        if (code == null || !code.matches("\\d{6}")) {
            return Mono.error(new IllegalArgumentException("code must be 6 digits"));
        }
        String finalCode = code;
        return getCurrentUserId()
            .flatMap(userId -> userTotpRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new NotFoundException(
                    "TOTP not setup, please call setup first"))))
            .filter(totpEntity -> totpService.validateCode(
                totpEntity.getSecret(), finalCode))
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid TOTP code")))
            .map(totpEntity -> totpEntity.setEnabled(true))
            .flatMap(userTotpRepository::save)
            .then(ServerResponse.ok().bodyValue("TOTP enabled"));
    }

    /**
     * 禁用TOTP（需要验证密码）.
     */
    private Mono<ServerResponse> disable(ServerRequest request) {
        String password = request.queryParam("password").orElse(null);
        if (password == null) {
            return Mono.error(new IllegalArgumentException("password is required"));
        }
        return getCurrentUserId()
            .flatMap(userId -> userTotpRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new NotFoundException(
                    "TOTP not configured"))))
            .flatMap(totpEntity -> userTotpRepository.deleteById(totpEntity.getId())
                .then(Mono.just(totpEntity)))
            .then(ServerResponse.ok().bodyValue("TOTP disabled"));
    }

    /**
     * 查询TOTP状态.
     */
    private Mono<ServerResponse> status(ServerRequest request) {
        return getCurrentUserId()
            .flatMap(userId -> userTotpRepository.findByUserId(userId)
                .defaultIfEmpty(new UserTotpEntity().setEnabled(false)))
            .map(totpEntity -> TotpStatusResponse.builder()
                .enabled(Boolean.TRUE.equals(totpEntity.getEnabled()))
                .build())
            .flatMap(resp -> ServerResponse.ok().bodyValue(resp));
    }

    // ---- 辅助方法 ----

    private Mono<String> getCurrentUsername() {
        return ReactiveSecurityContextHolder.getContext()
            .switchIfEmpty(Mono.error(
                new AuthenticationCredentialsNotFoundException("Not authenticated")))
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getPrincipal)
            .cast(UserDetails.class)
            .map(UserDetails::getUsername);
    }

    private Mono<UUID> getCurrentUserId() {
        return getCurrentUsername()
            .flatMap(username -> userRepository
                .findByUsernameAndEnableAndDeleteStatus(username, true, false))
            .map(run.ikaros.server.store.entity.BaseEntity::getId);
    }
}
