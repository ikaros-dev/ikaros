package run.ikaros.server.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import run.ikaros.api.store.entity.Authority;
import run.ikaros.api.store.entity.User;

class SecurityUserTest {

    @Test
    void constructor_shouldWrapUserAndAuthorities() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("secret");
        user.setEnable(true);
        user.setNonLocked(true);

        Authority authority = new Authority();
        authority.setType("ALL");
        authority.setTarget("*");
        authority.setAuthority("*");
        List<IkarosGrantedAuthority> authorities =
            List.of(new IkarosGrantedAuthority(authority));

        SecurityUser securityUser = new SecurityUser(user, authorities);

        assertEquals("testuser", securityUser.getUsername());
        assertEquals("secret", securityUser.getPassword());
        assertEquals(1L, securityUser.getId());
        assertTrue(securityUser.isEnabled());
        assertTrue(securityUser.isAccountNonLocked());
        assertEquals(1, securityUser.getAuthorities().size());
    }

    @Test
    void eraseCredentials_shouldSetPasswordToNull() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("secret");

        SecurityUser securityUser = new SecurityUser(user, List.of());
        assertEquals("secret", securityUser.getPassword());

        securityUser.eraseCredentials();

        assertNull(securityUser.getPassword());
    }

    @Test
    void isAccountNonLocked_shouldDelegateToUser() {
        User user = new User();
        user.setUsername("lockeduser");
        user.setPassword("pass");
        user.setNonLocked(false);

        SecurityUser securityUser = new SecurityUser(user, List.of());
        assertFalse(securityUser.isAccountNonLocked());
    }

    @Test
    void isEnabled_shouldDelegateToUser() {
        User user = new User();
        user.setUsername("disableduser");
        user.setPassword("pass");
        user.setEnable(false);
        user.setNonLocked(true);

        SecurityUser securityUser = new SecurityUser(user, List.of());
        assertFalse(securityUser.isEnabled());
    }

    @Test
    void getAuthorities_shouldReturnGrantedAuthorities() {
        Authority a1 = new Authority();
        a1.setType("API");
        a1.setTarget("/api/**");
        a1.setAuthority("*");
        Authority a2 = new Authority();
        a2.setType("ALL");
        a2.setTarget("*");
        a2.setAuthority("HTTP_GET");

        User user = new User();
        user.setUsername("authuser");
        user.setPassword("pass");

        SecurityUser securityUser = new SecurityUser(user,
            List.of(new IkarosGrantedAuthority(a1),
                new IkarosGrantedAuthority(a2)));

        assertEquals(2, securityUser.getAuthorities().size());
        assertTrue(securityUser.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().contains("API")));
        assertTrue(securityUser.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().contains("ALL")));
    }

    @Test
    void getUser_shouldReturnWrappedUser() {
        User user = new User();
        user.setId(42L);
        user.setUsername("wrapper");

        SecurityUser securityUser = new SecurityUser(user, List.of());
        assertEquals(user, securityUser.getUser());
        assertEquals(42L, securityUser.getUser().getId());
    }
}
