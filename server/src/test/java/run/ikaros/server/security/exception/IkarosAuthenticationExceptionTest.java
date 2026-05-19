package run.ikaros.server.security.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class IkarosAuthenticationExceptionTest {

    @Test
    void constructorWithMessage() {
        IkarosAuthenticationException ex =
            new IkarosAuthenticationException("auth failed");

        assertThat(ex.getMessage()).isEqualTo("auth failed");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndHttpStatus() {
        IkarosAuthenticationException ex =
            new IkarosAuthenticationException("forbidden", HttpStatus.FORBIDDEN);

        assertThat(ex.getMessage()).isEqualTo("forbidden");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        IkarosAuthenticationException ex =
            new IkarosAuthenticationException("auth failed", cause);

        assertThat(ex.getMessage()).isEqualTo("auth failed");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void constructorWithMessageCauseAndHttpStatus() {
        RuntimeException cause = new RuntimeException("root cause");
        IkarosAuthenticationException ex =
            new IkarosAuthenticationException("gone", cause, HttpStatus.GONE);

        assertThat(ex.getMessage()).isEqualTo("gone");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void constructorWithAllParameters() {
        RuntimeException cause = new RuntimeException("root cause");
        IkarosAuthenticationException ex =
            new IkarosAuthenticationException("msg", cause, false, true,
                HttpStatus.SERVICE_UNAVAILABLE);

        assertThat(ex.getMessage()).isEqualTo("msg");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void constructorWithSuppressionAndStackTrace() {
        RuntimeException cause = new RuntimeException("root cause");
        IkarosAuthenticationException ex =
            new IkarosAuthenticationException("msg", cause, true, false);

        assertThat(ex.getMessage()).isEqualTo("msg");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void defaultHttpStatusIsUnauthorized() {
        IkarosAuthenticationException ex =
            new IkarosAuthenticationException("test");

        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void isRuntimeException() {
        IkarosAuthenticationException ex =
            new IkarosAuthenticationException("test");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
