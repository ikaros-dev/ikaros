package run.ikaros.server.security.exception;

import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.model.ResponseResult;
import run.ikaros.server.infra.utils.JsonUtils;

public class JsonServerAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        //CommonResult result = new CommonResult();
        //result.setMessage("[" + ex.getClass().getSimpleName() + "] " + ex.getMessage());
        String resultJson = JsonUtils.obj2Json(
            ResponseResult.notAccess(
                "[" + ex.getClass().getSimpleName() + "] " + ex.getMessage(),
                ex));
        if (StringUtils.isBlank(resultJson)) {
            resultJson = "Obj to json fail.";
        }
        byte[] bytes = resultJson.getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(dataBuffer));
    }
}
