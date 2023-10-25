package run.ikaros.server.security.authentication.formlogin;

import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
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
import run.ikaros.api.infra.model.ResponseResult;
import run.ikaros.server.infra.utils.JsonUtils;

public class FormLoginFailureHandler implements ServerAuthenticationFailureHandler {
    private final ServerAuthenticationFailureHandler defaultHandler =
        new RedirectServerAuthenticationFailureHandler(AppConst.LOGIN_FAILURE_LOCATION);

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange,
                                              AuthenticationException ex) {
        ServerHttpResponse response = webFilterExchange.getExchange().getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        //CommonResult result = new CommonResult();
        //result.setMessage("[" + ex.getClass().getSimpleName() + "] " + ex.getMessage());
        String resultJson = JsonUtils.obj2Json(
            ResponseResult.notAccess("[" + ex.getClass().getSimpleName() + "] " + ex.getMessage(),
                ex)
        );
        if (StringUtils.isBlank(resultJson)) {
            resultJson = "Obj to json fail.";
        }
        byte[] bytes = resultJson.getBytes(StandardCharsets.UTF_8);
        DataBuffer dataBuffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(dataBuffer));
    }
}
