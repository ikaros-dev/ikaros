package run.ikaros.server.security.authentication.formlogin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import run.ikaros.api.wrap.CommonResult;
import run.ikaros.server.infra.utils.JsonUtils;

@Component
public class FormLoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException ex)
        throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON.toString());
        CommonResult result = new CommonResult();
        result.setException(ex);
        result.setMessage(ex.getLocalizedMessage());
        String json = JsonUtils.obj2Json(result);
        assert json != null;
        response.getWriter().write(json);
    }
}
