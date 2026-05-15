package run.ikaros.server.security.authentication.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import run.ikaros.api.infra.exception.security.UserAuthenticationException;
import run.ikaros.api.store.entity.User;
import run.ikaros.server.security.SecurityUser;

class JwtAuthenticationManagerTest {

    private UserDetailsService userDetailsService;
    private PasswordEncoder passwordEncoder;
    private JwtAuthenticationManager manager;

    @BeforeEach
    void setUp() {
        userDetailsService = mock(UserDetailsService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        manager = new JwtAuthenticationManager(userDetailsService, passwordEncoder);
    }

    private SecurityUser createSecurityUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEnable(true);
        user.setNonLocked(true);
        return new SecurityUser(user, List.of());
    }

    @Test
    void authenticate_validCredentials_shouldReturnAuthentication() {
        SecurityUser securityUser = createSecurityUser("testuser", "encoded_pass");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(securityUser);
        when(passwordEncoder.matches("raw_pass", "encoded_pass")).thenReturn(true);

        UsernamePasswordAuthenticationToken token =
            new UsernamePasswordAuthenticationToken("testuser", "raw_pass");

        Authentication result = manager.authenticate(token);

        assertNotNull(result);
        assertEquals("testuser", result.getName());
        assertEquals(securityUser, result.getPrincipal());
        assertEquals("raw_pass", result.getCredentials());
    }

    @Test
    void authenticate_wrongPassword_shouldThrowUserAuthenticationException() {
        SecurityUser securityUser = createSecurityUser("testuser", "encoded_pass");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(securityUser);
        when(passwordEncoder.matches("wrong_pass", "encoded_pass")).thenReturn(false);

        UsernamePasswordAuthenticationToken token =
            new UsernamePasswordAuthenticationToken("testuser", "wrong_pass");

        assertThrows(UserAuthenticationException.class,
            () -> manager.authenticate(token));
    }

    @Test
    void authenticate_nonExistentUser_shouldThrow() {
        when(userDetailsService.loadUserByUsername("nobody"))
            .thenThrow(new org.springframework.security.core.userdetails
                .UsernameNotFoundException("nobody"));

        UsernamePasswordAuthenticationToken token =
            new UsernamePasswordAuthenticationToken("nobody", "pass");

        assertThrows(org.springframework.security.core.userdetails
            .UsernameNotFoundException.class,
            () -> manager.authenticate(token));
    }

    @Test
    void authenticate_emptyPassword_shouldThrow() {
        SecurityUser securityUser = createSecurityUser("testuser", "encoded_pass");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(securityUser);
        when(passwordEncoder.matches("", "encoded_pass")).thenReturn(false);

        UsernamePasswordAuthenticationToken token =
            new UsernamePasswordAuthenticationToken("testuser", "");

        assertThrows(UserAuthenticationException.class,
            () -> manager.authenticate(token));
    }
}
