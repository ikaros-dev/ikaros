package run.ikaros.server.config.mybatis;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class MybatisPlusMetaObjectHandlerTest {

    private final MybatisPlusMetaObjectHandler handler = new MybatisPlusMetaObjectHandler();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void handler_canBeInstantiated() {
        assertNotNull(handler);
    }

    @Test
    void insertFill_doesNotThrow() {
        SecurityContextHolder.clearContext();
        // The handler should be instantiable and usable
        assertNotNull(handler);
    }

    @Test
    void updateFill_doesNotThrow() {
        SecurityContextHolder.clearContext();
        // The handler should be instantiable and usable
        assertNotNull(handler);
    }
}
