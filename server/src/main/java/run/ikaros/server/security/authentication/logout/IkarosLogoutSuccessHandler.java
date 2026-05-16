package run.ikaros.server.security.authentication.logout;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import run.ikaros.api.wrap.CommonResult;
import run.ikaros.server.infra.utils.JsonUtils;

public class IkarosLogoutSuccessHandler implements LogoutSuccessHandler {
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                @Nullable Authentication authentication)
        throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON.toString());
        CommonResult result = new CommonResult();
        result.setCustomMessage("LOGOUT SUCCESS");
        String json = JsonUtils.obj2Json(result);
        assert json != null;
        response.getWriter().write(json);
    }

}
