package run.ikaros.server.security.authentication.jwt;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import run.ikaros.api.store.entity.User;
import run.ikaros.server.security.IkarosGrantedAuthority;
import run.ikaros.server.security.SecurityUser;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationProvider jwtProvider;
    private JwtAuthenticationFilter filter;
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        jwtProvider = mock(JwtAuthenticationProvider.class);
        userDetailsService = mock(
            org.springframework.security.core.userdetails.UserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtProvider, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_noAuthHeader_shouldNotSetAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(jwtProvider, never()).validateToken(anyString());
    }

    @Test
    void doFilter_nonBearerToken_shouldNotSetAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(jwtProvider, never()).validateToken(anyString());
    }

    @Test
    void doFilter_validBearerToken_shouldSetAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid.jwt.token");
        when(jwtProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtProvider.extractUsername("valid.jwt.token")).thenReturn("testuser");

        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encoded");
        user.setEnable(true);
        user.setNonLocked(true);
        SecurityUser securityUser = new SecurityUser(user, List.of());
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(securityUser);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        org.springframework.security.core.Authentication auth =
            SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        assert auth.getPrincipal().equals(securityUser);
    }

    @Test
    void doFilter_invalidBearerToken_shouldNotSetAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer bad.token");
        when(jwtProvider.validateToken("bad.token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    void doFilter_emptyBearerToken_shouldNotSetAuthentication() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer ");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        // Empty token after "Bearer " should fail validation
        verify(jwtProvider).validateToken("");
    }
}
