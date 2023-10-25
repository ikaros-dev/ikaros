package run.ikaros.server.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginRuntimeException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.wrap.CommonResult;
import run.ikaros.server.infra.utils.JsonUtils;

@Slf4j
@Component
public class GlobalExceptionHandlerConfig implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
            .switchIfEmpty(Mono.error(new NotFoundException(
                "Data not found for Url: " + exchange.getRequest().getURI())))
            .onErrorResume(NotFoundException.class,
                e1 -> writeResponse(exchange.getResponse(), e1, HttpStatus.NOT_FOUND))
            .onErrorResume(RuntimeException.class,
                e2 -> writeResponse(exchange.getResponse(), e2, HttpStatus.BAD_REQUEST))
            .onErrorResume(AuthenticationException.class,
                e3 -> writeResponse(exchange.getResponse(), e3, HttpStatus.FORBIDDEN))
            .onErrorResume(PluginRuntimeException.class,
                e4 -> writeResponse(exchange.getResponse(), e4, HttpStatus.BAD_REQUEST))
            .onErrorResume(IllegalArgumentException.class,
                e5 -> writeResponse(exchange.getResponse(), e5, HttpStatus.BAD_REQUEST))
            .onErrorResume(DuplicateKeyException.class,
                e6 -> writeResponse(exchange.getResponse(), e6, HttpStatus.BAD_REQUEST)
                    .onErrorResume(Exception.class,
                        e -> writeResponse(exchange.getResponse(), e,
                            HttpStatus.INTERNAL_SERVER_ERROR)
                    ));
    }

    private static Mono<Void> writeResponse(ServerHttpResponse response,
                                            Throwable e, HttpStatus httpStatus) {
        if (!(e instanceof NotFoundException)) {
            log.error("[{}] {}", e.getClass().getSimpleName(), e.getLocalizedMessage(), e);
        }
        List<String> contextTypeList = response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        if (Objects.isNull(contextTypeList)) {
            try {
                response.getHeaders()
                    .add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString());
            } catch (UnsupportedOperationException e2) {
                // log.warn("add http header context type fail", e2);
            }
        }
        response.setStatusCode(httpStatus);
        CommonResult result = new CommonResult();
        result.setException(e.getClass().getName());
        result.setMessage(e.getLocalizedMessage());
        String json = JsonUtils.obj2Json(result);
        assert json != null;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(buffer));
    }
}
