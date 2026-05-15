package run.ikaros.server.security.authentication.logout;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class LogoutConfigurerTest {

    @Test
    void constructor_shouldNotBeNull() {
        LogoutConfigurer configurer = new LogoutConfigurer();
        assertNotNull(configurer);
    }
}
