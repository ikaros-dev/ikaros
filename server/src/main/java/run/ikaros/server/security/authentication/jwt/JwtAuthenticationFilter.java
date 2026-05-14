package run.ikaros.server.security.authentication.jwt;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.SecurityUser;

@Slf4j
@Component
public class JwtAuthenticationFilter implements Filter {
    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtAuthenticationProvider jwtAuthenticationProvider,
                                   UserDetailsService userDetailsService
    ) {
        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
        this.userDetailsService = userDetailsService;
    }


    private String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String token = extractToken(httpRequest.getHeader(HttpHeaders.AUTHORIZATION));
        if (token != null && jwtAuthenticationProvider.validateToken(token)) {
            String username = jwtAuthenticationProvider.extractUsername(token);
            SecurityUser securityUser =
                (SecurityUser) userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                    securityUser, null, securityUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            log.debug("Set user info to security context for username={}.", username);
        }
        chain.doFilter(request, response);
    }
}
