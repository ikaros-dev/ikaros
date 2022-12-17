package run.ikaros.server.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import run.ikaros.server.constants.HttpConst;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.result.ResultCode;

import java.io.IOException;

/**
 * 无权访问处理器
 *
 * @author guohao
 * @date 2022/09/11
 */
public class IkarosAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException)
        throws IOException, ServletException {
        CommonResult<Object> result =
            CommonResult.fail(ResultCode.UNAUTHORIZED, "UNAUTHORIZED - "
                + authException.getClass().getSimpleName() + " - "
                + authException.getMessage(), null);
        response.setContentType(HttpConst.CONTENT_TYPE_JSON);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().println(result);
    }
}
