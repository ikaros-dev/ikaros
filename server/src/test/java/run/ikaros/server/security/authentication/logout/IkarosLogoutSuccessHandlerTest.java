package run.ikaros.server.security.authentication.logout;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class IkarosLogoutSuccessHandlerTest {

    @Test
    void onLogoutSuccess_shouldReturnLogoutSuccessJson() throws Exception {
        IkarosLogoutSuccessHandler handler = new IkarosLogoutSuccessHandler();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        handler.onLogoutSuccess(request, response, null);

        verify(response).setContentType("application/json");
        String body = stringWriter.toString();
        assert body.contains("LOGOUT SUCCESS");
    }
}
