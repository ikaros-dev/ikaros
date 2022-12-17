package run.ikaros.server.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import run.ikaros.server.constants.HttpConst;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.result.ResultCode;

import java.io.IOException;

/**
 * @author guohao
 * @date 2022/09/11
 */
public class IkarosAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException)
        throws IOException, ServletException {
        CommonResult<Object> result =
            CommonResult.fail(ResultCode.FORBIDDEN, "FORBIDDEN - "
                + accessDeniedException.getClass().getSimpleName() + " - "
                + accessDeniedException.getMessage(), null);
        response.setContentType(HttpConst.CONTENT_TYPE_JSON);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.getWriter().println(result);
    }
}
