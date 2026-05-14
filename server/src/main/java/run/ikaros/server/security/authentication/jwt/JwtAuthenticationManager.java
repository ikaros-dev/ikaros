package run.ikaros.server.security.authentication.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import run.ikaros.api.infra.exception.security.UserAuthenticationException;
import run.ikaros.server.security.SecurityUser;

@Slf4j
@Component
public class JwtAuthenticationManager implements AuthenticationManager {
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public JwtAuthenticationManager(UserDetailsService userDetailsService,
                                    PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        final String username = authentication.getName();
        final String password = authentication.getCredentials().toString();
        SecurityUser securityUser = (SecurityUser) userDetailsService.loadUserByUsername(username);
        if (passwordEncoder.matches(password, securityUser.getPassword())) {
            return new UsernamePasswordAuthenticationToken(securityUser, password,
                securityUser.getAuthorities());
        } else {
            throw new UserAuthenticationException("Authorization fail, "
                + "invalid username or password.");
        }
    }
}
