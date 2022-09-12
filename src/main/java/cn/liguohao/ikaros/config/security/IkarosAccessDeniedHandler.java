package cn.liguohao.ikaros.config.security;

import cn.liguohao.ikaros.common.result.CommonResult;
import cn.liguohao.ikaros.common.result.ResultCode;
import cn.liguohao.ikaros.common.constants.HttpConstants;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

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
        response.setContentType(HttpConstants.CONTENT_TYPE_JSON);
        response.getWriter().println(result);
    }
}
