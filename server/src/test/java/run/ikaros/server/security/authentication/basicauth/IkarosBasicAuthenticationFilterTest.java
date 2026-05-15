package run.ikaros.server.security.authentication.basicauth;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import run.ikaros.api.store.entity.User;
import run.ikaros.server.security.SecurityUser;

class IkarosBasicAuthenticationFilterTest {

    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    private IkarosBasicAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        userDetailsService = mock(
            org.springframework.security.core.userdetails.UserDetailsService.class);
        filter = new IkarosBasicAuthenticationFilter(userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String base64Encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void doFilter_noAuthHeader_shouldNotSetAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void doFilter_basicAuth_shouldSetAuthentication() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final FilterChain chain = mock(FilterChain.class);

        String credentials = base64Encode("testuser:mypassword");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + credentials);

        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encoded");
        user.setEnable(true);
        user.setNonLocked(true);
        SecurityUser securityUser = new SecurityUser(user, List.of());
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(securityUser);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assert SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Test
    void doFilter_basicAuthWithoutColon_shouldNotSetAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        String invalidCredentials = base64Encode("userwithoutcolon");
        when(request.getHeader(HttpHeaders.AUTHORIZATION))
            .thenReturn("Basic " + invalidCredentials);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void doFilter_nonBasicAuth_shouldNotSetAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer some.token");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }
}
