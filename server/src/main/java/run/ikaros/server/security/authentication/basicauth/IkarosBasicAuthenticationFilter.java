package run.ikaros.server.security.authentication.basicauth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import run.ikaros.server.security.SecurityUser;

@Slf4j
@Component
public class IkarosBasicAuthenticationFilter implements Filter {
    private final UserDetailsService userDetailsService;

    public IkarosBasicAuthenticationFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }


    private String extractBasic64(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Basic ")) {
            return bearerToken.substring(6);
        }
        return null;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String basic64 =
            extractBasic64(httpRequest.getHeader(HttpHeaders.AUTHORIZATION));
        if (basic64 != null) {
            String decoder =
                new String(Base64.getDecoder().decode(basic64), StandardCharsets.UTF_8);
            if (!decoder.contains(":")) {
                chain.doFilter(request, response);
                return;
            }
            String[] split = decoder.split(":");
            String username = split[0];
            // String password = split[1];
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
