package run.ikaros.server.security.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class IkarosRootUserAccessDeniedExceptionTest {

    @Test
    void constructorWithMessage() {
        IkarosRootUserAccessDeniedException ex =
            new IkarosRootUserAccessDeniedException("access denied");

        assertThat(ex.getMessage()).isEqualTo("access denied");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndHttpStatus() {
        IkarosRootUserAccessDeniedException ex =
            new IkarosRootUserAccessDeniedException("unauthorized", HttpStatus.UNAUTHORIZED);

        assertThat(ex.getMessage()).isEqualTo("unauthorized");
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void constructorWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        IkarosRootUserAccessDeniedException ex =
            new IkarosRootUserAccessDeniedException("access denied", cause);

        assertThat(ex.getMessage()).isEqualTo("access denied");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void constructorWithMessageCauseAndHttpStatus() {
        RuntimeException cause = new RuntimeException("root cause");
        IkarosRootUserAccessDeniedException ex =
            new IkarosRootUserAccessDeniedException("gone", cause, HttpStatus.GONE);

        assertThat(ex.getMessage()).isEqualTo("gone");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void constructorWithAllParameters() {
        RuntimeException cause = new RuntimeException("root cause");
        IkarosRootUserAccessDeniedException ex =
            new IkarosRootUserAccessDeniedException("msg", cause, false, true,
                HttpStatus.SERVICE_UNAVAILABLE);

        assertThat(ex.getMessage()).isEqualTo("msg");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void constructorWithSuppressionAndStackTrace() {
        RuntimeException cause = new RuntimeException("root cause");
        IkarosRootUserAccessDeniedException ex =
            new IkarosRootUserAccessDeniedException("msg", cause, true, false);

        assertThat(ex.getMessage()).isEqualTo("msg");
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void defaultHttpStatusIsForbidden() {
        IkarosRootUserAccessDeniedException ex =
            new IkarosRootUserAccessDeniedException("test");

        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void isRuntimeException() {
        IkarosRootUserAccessDeniedException ex =
            new IkarosRootUserAccessDeniedException("test");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
