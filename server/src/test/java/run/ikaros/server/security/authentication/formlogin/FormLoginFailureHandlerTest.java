package run.ikaros.server.security.authentication.formlogin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class FormLoginFailureHandlerTest {

    private FormLoginFailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = new FormLoginFailureHandler();
    }

    @Test
    void onAuthenticationFailure_shouldReturn401WithJson() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        BadCredentialsException ex =
            new BadCredentialsException("Bad credentials");

        handler.onAuthenticationFailure(request, response, ex);

        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        String body = stringWriter.toString();
        assert body.contains("Bad credentials");
    }
}
