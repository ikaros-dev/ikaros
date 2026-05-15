package run.ikaros.server.security.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class SecurityExceptionTest {

    @Test
    void userNotFoundException_singleArg_shouldSetMessage() {
        UserNotFoundException ex = new UserNotFoundException("user not found");
        assertEquals("user not found", ex.getMessage());
    }

    @Test
    void userNotFoundException_withCause_shouldSetMessageAndCause() {
        IllegalArgumentException cause = new IllegalArgumentException("db error");
        UserNotFoundException ex = new UserNotFoundException("user not found", cause);
        assertEquals("user not found", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void roleNotFoundException_singleArg_shouldSetMessage() {
        RoleNotFoundException ex = new RoleNotFoundException("role not found");
        assertEquals("role not found", ex.getMessage());
    }

    @Test
    void roleNotFoundException_withCause_shouldSetMessageAndCause() {
        RuntimeException cause = new RuntimeException("db error");
        RoleNotFoundException ex = new RoleNotFoundException("role not found", cause);
        assertEquals("role not found", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void userHasNotRoleException_singleArg_shouldSetMessage() {
        UserHasNotRoleException ex = new UserHasNotRoleException("no role");
        assertEquals("no role", ex.getMessage());
    }

    @Test
    void userHasNotRoleException_withCause_shouldSetMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("role deleted");
        UserHasNotRoleException ex = new UserHasNotRoleException("no role", cause);
        assertEquals("no role", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void allExceptions_shouldBeAuthenticationException() {
        UserNotFoundException ex1 = new UserNotFoundException("msg");
        RoleNotFoundException ex2 = new RoleNotFoundException("msg");
        UserHasNotRoleException ex3 = new UserHasNotRoleException("msg");

        assertNotNull(ex1);
        assertNotNull(ex2);
        assertNotNull(ex3);
    }
}
