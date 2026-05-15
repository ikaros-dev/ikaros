package run.ikaros.server.core.authority;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.fn.builders.requestbody.Builder;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.authority.Authority;
import run.ikaros.api.core.authority.AuthorityCondition;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.api.wrap.PagingWrap;
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

            .POST("/authority", this::postAuthority,
                builder -> builder.operationId("CreateAuthority")
                    .tag(tag).description("Create authority")
                    .requestBody(Builder.requestBodyBuilder()
                        .implementation(Authority.class)))

            .DELETE("/authority/id/{id}", this::deleteAuthorityById,
                builder -> builder.operationId("DeleteAuthorityById")
                    .tag(tag).description("Delete authority by id")
                    .parameter(parameterBuilder()
                        .in(ParameterIn.PATH)
                        .required(true)
                        .name("id")))

            .GET("/authorities/condition", this::findAuthoritiesByCondition,
                builder -> builder.operationId("GetAuthoritiesByCondition")
                    .tag(tag).description("Get authorities by condition")
                    .parameter(parameterBuilder()
                        .name("allow").description("是否放行")
                        .in(ParameterIn.QUERY).implementation(Boolean.class))
                    .parameter(parameterBuilder().required(true)
                        .name("type").description("权限的类型")
                        .in(ParameterIn.QUERY).implementation(AuthorityType.class))
                    .parameter(parameterBuilder()
                        .name("target").description("权限的目标方，一般是路径")
                        .in(ParameterIn.QUERY).implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("authority")
                        .description("操作目标的方式，一般是HTTP的方法")
                        .in(ParameterIn.QUERY).implementation(String.class))
                    .parameter(parameterBuilder()
                        .name("page")
                        .description("第几页，从1开始, 默认为1.")
                        .implementation(Integer.class))
                    .parameter(parameterBuilder()
                        .name("size")
                        .description("每页条数，默认为10.")
                        .implementation(Integer.class))

                    .response(responseBuilder()
                        .implementation(PagingWrap.class)))

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

    private Mono<ServerResponse> postAuthority(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(Authority.class)
            .flatMap(authorityService::save)
            .flatMap(authority -> ServerResponse.ok().bodyValue(authority));
    }

    private Mono<ServerResponse> deleteAuthorityById(ServerRequest serverRequest) {
        UUID id = UuidV7Utils.fromString((serverRequest.pathVariable("id")));
        return authorityService.deleteById(id)
            .then(ServerResponse.ok().bodyValue(id));
    }

    private Mono<ServerResponse> findAuthoritiesByCondition(ServerRequest serverRequest) {
        AuthorityType type = AuthorityType.valueOf(serverRequest.queryParam("type").orElse("ALL"));
        Boolean allow = Boolean.valueOf(serverRequest.queryParam("allow").orElse("true"));
        String target = serverRequest.queryParam("target").orElse("");
        String authority = serverRequest.queryParam("authority").orElse("");
        Integer page = Integer.valueOf(serverRequest.queryParam("page").orElse("1"));
        Integer size = Integer.valueOf(serverRequest.queryParam("size").orElse("20"));
        AuthorityCondition condition = AuthorityCondition.builder().page(page).size(size)
            .type(type).allow(allow).target(target).authority(authority).build();
        return authorityService.findAllByCondition(condition)
            .flatMap(pagingWarp -> ServerResponse.ok().bodyValue(pagingWarp));
    }
}
