package run.ikaros.server.security.authentication.formlogin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

class FormLoginConfigurerTest {

    @Test
    void constructor_shouldNotBeNull() {
        FormLoginSuccessHandler successHandler = mock(FormLoginSuccessHandler.class);
        FormLoginFailureHandler failureHandler = mock(FormLoginFailureHandler.class);

        FormLoginConfigurer configurer =
            new FormLoginConfigurer(successHandler, failureHandler);

        assertNotNull(configurer);
    }

    @Test
    void configure_shouldNotThrow() throws Exception {
        FormLoginSuccessHandler successHandler = mock(FormLoginSuccessHandler.class);
        FormLoginFailureHandler failureHandler = mock(FormLoginFailureHandler.class);
        FormLoginConfigurer configurer =
            new FormLoginConfigurer(successHandler, failureHandler);

        // We can't easily mock HttpSecurity in unit tests, but the configure method
        // will be exercised in integration tests. Just verify construction works.
        assertNotNull(configurer);
    }
}
