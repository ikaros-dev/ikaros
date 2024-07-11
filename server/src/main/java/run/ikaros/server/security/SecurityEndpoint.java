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
import run.ikaros.server.endpoint.CoreEndpoint;
import run.ikaros.server.security.authentication.jwt.JwtApplyParam;
import run.ikaros.server.security.authentication.jwt.JwtAuthenticationProvider;
import run.ikaros.server.security.authentication.jwt.JwtReactiveAuthenticationManager;

@Slf4j
@Component
public class SecurityEndpoint implements CoreEndpoint {
    private final JwtReactiveAuthenticationManager authenticationManager;
    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final ReactiveUserDetailsService userDetailsService;

    /**
     * Construct.
     */
    public SecurityEndpoint(
        JwtReactiveAuthenticationManager authenticationManager,
        JwtAuthenticationProvider jwtAuthenticationProvider,
        ReactiveUserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
        this.userDetailsService = userDetailsService;
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
                        .implementation(String.class)
                        .description("Token"))
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
            .map(UserDetails::getUsername)
            .map(jwtAuthenticationProvider::generateToken)
            .flatMap(token -> ServerResponse.ok().bodyValue(token))
            .onErrorResume(UserNotFoundException.class,
                e -> Mono.error(new UserAuthenticationException(e.getLocalizedMessage(), e)));
    }
}
