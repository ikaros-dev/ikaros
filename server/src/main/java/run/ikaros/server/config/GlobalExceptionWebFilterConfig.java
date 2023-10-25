package run.ikaros.server.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.exception.IkarosException;
import run.ikaros.api.infra.exception.IkarosNotFoundException;
import run.ikaros.api.infra.exception.IkarosPluginException;
import run.ikaros.api.infra.model.ResponseResult;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.server.infra.utils.JsonUtils;

@Slf4j
@Component
public class GlobalExceptionWebFilterConfig implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
            .switchIfEmpty(Mono.error(new IkarosNotFoundException(
                "Data not found for Url: " + exchange.getRequest().getURI())))
            .onErrorResume(IkarosNotFoundException.class,
                e1 -> writeResponse(exchange.getResponse(), e1,
                    ResponseResult.notFound(e1.getMessage(), e1)))
            .onErrorResume(IkarosException.class,
                e2 -> writeResponse(exchange.getResponse(), e2,
                    ResponseResult.fail(e2.getMessage(), e2)))
            .onErrorResume(AuthenticationException.class,
                e3 -> writeResponse(exchange.getResponse(), e3,
                    ResponseResult.notAccess(e3.getMessage(), e3)))
            .onErrorResume(IkarosPluginException.class,
                e4 -> writeResponse(exchange.getResponse(), e4,
                    ResponseResult.fail(e4.getMessage(), e4)))
            .onErrorResume(IllegalArgumentException.class,
                e5 -> writeResponse(exchange.getResponse(), e5,
                    ResponseResult.badRequest(e5.getMessage(), e5)))
            .onErrorResume(DuplicateKeyException.class,
                e6 -> writeResponse(exchange.getResponse(), e6,
                    ResponseResult.recordExists(e6.getMessage(), e6)))
            .onErrorResume(Exception.class,
                e -> writeResponse(exchange.getResponse(), e,
                    ResponseResult.unknown(e.getMessage(), e))

            );
    }

    private static Mono<Void> writeResponse(ServerHttpResponse response, Throwable e, Object data) {
        log.error("[{}] {}", e.getClass().getSimpleName(), e.getLocalizedMessage(), e);
        List<String> contextTypeList = response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        if (Objects.isNull(contextTypeList)) {
            response.getHeaders().add("Content-Type", "application/json");
        }
        response.setStatusCode(HttpStatus.OK);
        String rspResult = JsonUtils.obj2Json(data);
        assert rspResult != null;
        byte[] bytes = rspResult.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(buffer));
    }
}
