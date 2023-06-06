package run.ikaros.server.core.user;

import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.apiresponse.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
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
        var tag = OpenApiConst.CORE_VERSION + "/User";
        return SpringdocRouteBuilder.route()
            .GET("/user/current", this::getCurrentUserDetail,
                builder -> builder.operationId("GetCurrentUserDetail")
                    .tag(tag)
                    .description("Get current user detail.")
                    .response(Builder.responseBuilder()
                        .implementation(User.class)))
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
}
