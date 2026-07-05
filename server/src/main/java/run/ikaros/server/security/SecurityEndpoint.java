package run.ikaros.server.security;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static run.ikaros.server.security.authentication.jwt.JwtApplyParam.Type.USERNAME_PASSWORD;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.infra.exception.security.UserAuthenticationException;
import run.ikaros.api.infra.exception.user.UserNotFoundException;
import run.ikaros.server.core.user.User;
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.security.authentication.jwt.JwtApplyParam;
import run.ikaros.server.security.authentication.jwt.JwtApplyResponse;
import run.ikaros.server.security.authentication.jwt.JwtAuthenticationProvider;
import run.ikaros.server.security.authentication.jwt.JwtReactiveAuthenticationManager;
import run.ikaros.server.security.authentication.totp.TOTPService;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.entity.UserTotpEntity;
import run.ikaros.server.store.repository.UserTotpRepository;

@Slf4j
@Component
public class SecurityEndpoint implements CoreEndpoint {
    private final JwtReactiveAuthenticationManager authenticationManager;
    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final ReactiveUserDetailsService userDetailsService;
    private final TOTPService totpService;
    private final UserTotpRepository userTotpRepository;

    /**
     * Construct.
     */
    public SecurityEndpoint(
        JwtReactiveAuthenticationManager authenticationManager,
        JwtAuthenticationProvider jwtAuthenticationProvider,
        ReactiveUserDetailsService userDetailsService,
        TOTPService totpService,
        UserTotpRepository userTotpRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
        this.userDetailsService = userDetailsService;
        this.totpService = totpService;
        this.userTotpRepository = userTotpRepository;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/security";
        return SpringdocRouteBuilder.route()
            .POST("/security/auth/token/jwt/apply", this::applyJwtToken,
                builder -> builder.operationId("ApplyJwtToken")
                    .tag(tag).description("Apply JWT token")
                    .requestBody(requestBodyBuilder()
                        .implementation(JwtApplyParam.class)
                        .description("Apply JWT token params"))
                    .response(responseBuilder()
                        .implementation(JwtApplyResponse.class)
                        .description("Jwt token response."))
            )
            .PUT("/security/auth/token/jwt/refresh", this::refreshToken,
                builder -> builder.operationId("RefreshToken")
                    .tag(tag).description("Refresh access token with refresh token.")
                    .requestBody(requestBodyBuilder().implementation(String.class)
                        .description("Refresh token."))
                    .response(responseBuilder().implementation(String.class)
                        .description("New access token."))
            )
            .build();
    }

    private Mono<ServerResponse> applyJwtToken(ServerRequest request) {
        return request.bodyToMono(JwtApplyParam.class)
            .filter(jwtApplyParam -> jwtApplyParam.getAuthType() == USERNAME_PASSWORD)
            .map(jwtApplyParam -> new UsernamePasswordAuthenticationToken(
                jwtApplyParam.getUsername(), jwtApplyParam.getPassword()))
            .flatMap(authenticationManager::authenticate)
            .map(Authentication::getPrincipal)
            .filter(principal -> (principal instanceof UserDetails))
            .map(principal -> (UserDetails) principal)
            .map(UserDetails::getUsername)
            .map(String::valueOf)
            .flatMap(userDetailsService::findByUsername)
            .flatMap(userDetails -> userTotpRepository.findByUserId(
                    ((UserEntity) userDetails).getId())
                .defaultIfEmpty(new UserTotpEntity().setEnabled(false))
                .flatMap(totpEntity -> {
                    if (Boolean.TRUE.equals(totpEntity.getEnabled())) {
                        // 用户已启用TOTP，返回临时令牌
                        String tempToken =
                            jwtAuthenticationProvider.generateTempToken(userDetails.getUsername());
                        return Mono.just(JwtApplyResponse.builder()
                            .username(userDetails.getUsername())
                            .totpRequired(true)
                            .tempToken(tempToken)
                            .build());
                    }
                    // 未启用TOTP，正常返回JWT
                    return jwtAuthenticationProvider.generateJwtResp(userDetails);
                }))
            .flatMap(token -> ServerResponse.ok().bodyValue(token))
            .onErrorResume(UserNotFoundException.class,
                e -> Mono.error(new UserAuthenticationException(e.getLocalizedMessage(), e)));
    }

    private Mono<ServerResponse> refreshToken(ServerRequest request) {
        return request.bodyToMono(String.class)
            .flatMap(jwtAuthenticationProvider::refreshToken)
            .flatMap(accessToken -> ServerResponse.ok().bodyValue(accessToken));
    }
}
