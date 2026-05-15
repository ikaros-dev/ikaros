package run.ikaros.server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

    @Test
    void defaults_shouldHaveCorrectExpiryValues() {
        SecurityProperties properties = new SecurityProperties();

        assertNotNull(properties.getExpiry());
        assertEquals(3, properties.getExpiry().getAccessTokenDay());
        assertEquals(3, properties.getExpiry().getRefreshTokenMonth());
    }

    @Test
    void defaults_shouldHaveCorrectInitializerValues() {
        SecurityProperties properties = new SecurityProperties();

        assertNotNull(properties.getInitializer());
        assertEquals("tomoki", properties.getInitializer().getMasterUsername());
        assertEquals("桜井智樹", properties.getInitializer().getMasterNickname());
        assertFalse(properties.getInitializer().isDisabled());
        assertNull(properties.getInitializer().getMasterPassword());
    }

    @Test
    void expiry_shouldBeSettable() {
        SecurityProperties properties = new SecurityProperties();

        properties.getExpiry().setAccessTokenDay(7);
        properties.getExpiry().setRefreshTokenMonth(6);

        assertEquals(7, properties.getExpiry().getAccessTokenDay());
        assertEquals(6, properties.getExpiry().getRefreshTokenMonth());
    }

    @Test
    void initializer_shouldBeSettable() {
        SecurityProperties properties = new SecurityProperties();

        properties.getInitializer().setMasterUsername("admin");
        properties.getInitializer().setMasterNickname("Administrator");
        properties.getInitializer().setMasterPassword("secret123");
        properties.getInitializer().setDisabled(true);

        assertEquals("admin", properties.getInitializer().getMasterUsername());
        assertEquals("Administrator", properties.getInitializer().getMasterNickname());
        assertEquals("secret123", properties.getInitializer().getMasterPassword());
        assertEquals(true, properties.getInitializer().isDisabled());
    }
}
