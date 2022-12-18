package run.ikaros.server.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;
import run.ikaros.server.constants.SecurityConst;
import run.ikaros.server.utils.JwtUtils;

import java.io.IOException;


public class JwtAuthorizationFilter extends BasicAuthenticationFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthorizationFilter.class);

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
        throws ServletException, IOException {
        String token = this.getTokenFromHttpRequest(request);
        if (StringUtils.hasText(token) && JwtUtils.validateToken(token)) {
            Authentication authentication = JwtUtils.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private String getTokenFromHttpRequest(HttpServletRequest request) {
        String authorization = request.getHeader(SecurityConst.TOKEN_HEADER);
        if (authorization == null || !authorization.startsWith(SecurityConst.TOKEN_PREFIX)) {
            return null;
        }
        return authorization.replace(SecurityConst.TOKEN_PREFIX, "");
    }

}
