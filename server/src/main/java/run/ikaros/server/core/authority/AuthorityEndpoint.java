package run.ikaros.server.core.authority;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.security.Authority;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.endpoint.CoreEndpoint;

@Slf4j
@Component
public class AuthorityEndpoint implements CoreEndpoint {
    private final AuthorityService authorityService;

    public AuthorityEndpoint(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/authority";
        return SpringdocRouteBuilder.route()
            .GET("/authority/types", this::getAuthorityTypes,
                builder -> builder.operationId("GetAuthorityTypes")
                    .tag(tag).description("Get authority types")
                    .response(responseBuilder()
                        .implementationArray(String.class)))

            .GET("/authorities/type/{type}", this::getAuthoritiesByType,
                builder -> builder.operationId("GetAuthoritiesByType")
                    .tag(tag).description("Get authorities by type")
                    .parameter(parameterBuilder()
                        .name("type").required(true)
                        .in(ParameterIn.PATH)
                        .implementation(AuthorityType.class))
                    .response(responseBuilder()
                        .implementationArray(Authority.class)))

            .build();
    }

    private Mono<ServerResponse> getAuthorityTypes(ServerRequest serverRequest) {
        return authorityService.getAuthorityTypes()
            .collectList()
            .flatMap(types -> ServerResponse.ok().bodyValue(types));
    }

    private Mono<ServerResponse> getAuthoritiesByType(ServerRequest serverRequest) {
        AuthorityType type = AuthorityType.valueOf(serverRequest.pathVariable("type"));
        return authorityService.getAuthoritiesByType(type)
            .collectList()
            .flatMap(authorities -> ServerResponse.ok().bodyValue(authorities));
    }
}
