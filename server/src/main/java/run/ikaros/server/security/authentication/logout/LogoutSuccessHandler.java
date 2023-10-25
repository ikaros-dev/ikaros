package run.ikaros.server.security.authentication.logout;

import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.model.ResponseResult;
import run.ikaros.server.infra.utils.JsonUtils;

public class LogoutSuccessHandler implements ServerLogoutSuccessHandler {

    @Override
    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange,
                                      Authentication authentication) {
        ServerHttpResponse response = exchange.getExchange().getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        //CommonResult result = new CommonResult();
        //result.setMessage("LOGOUT SUCCESS");
        String resultJson = JsonUtils.obj2Json(
            ResponseResult.success(authentication)
        );
        if (StringUtils.isBlank(resultJson)) {
            resultJson = "Obj to json fail.";
        }
        byte[] bytes = resultJson.getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(dataBuffer));
    }
}
