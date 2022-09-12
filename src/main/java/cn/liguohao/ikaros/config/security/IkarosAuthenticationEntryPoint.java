package cn.liguohao.ikaros.config.security;

import cn.liguohao.ikaros.common.result.CommonResult;
import cn.liguohao.ikaros.common.result.ResultCode;
import cn.liguohao.ikaros.common.constants.HttpConstants;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

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
        response.setContentType(HttpConstants.CONTENT_TYPE_JSON);
        response.getWriter().println(result);
    }
}
