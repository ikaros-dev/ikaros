package run.ikaros.server.security.authentication.formlogin;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.wrap.CommonResult;
import run.ikaros.server.infra.utils.JsonUtils;

public class FormLoginFailureHandler implements ServerAuthenticationFailureHandler {
    private final ServerAuthenticationFailureHandler defaultHandler =
        new RedirectServerAuthenticationFailureHandler(AppConst.LOGIN_FAILURE_LOCATION);

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange,
                                              AuthenticationException ex) {
        ServerHttpResponse response = webFilterExchange.getExchange().getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        CommonResult result = new CommonResult();
        result.setException(ex.getClass().getName());
        result.setMessage(ex.getLocalizedMessage());
        String json = JsonUtils.obj2Json(result);
        assert json != null;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(dataBuffer));
    }
}
