package run.ikaros.server.security.authentication.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.exception.user.UserAuthException;
import run.ikaros.api.infra.exception.user.UserNotFoundException;

@Slf4j
@Component
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {
    private final ReactiveUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public JwtReactiveAuthenticationManager(ReactiveUserDetailsService userDetailsService,
                                            PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        final String username = authentication.getName();
        final String password = authentication.getCredentials().toString();
        return userDetailsService.findByUsername(username)
            .switchIfEmpty(Mono.error(
                new UserNotFoundException("User for username[" + username + "] not found, "
                    + "may be disabled or not exists。")))
            .filter(userDetails -> passwordEncoder.matches(password, userDetails.getPassword()))
            .switchIfEmpty(Mono.error(new UserAuthException("Authorization fail, "
                + "invalid username or password.")))
            .map(userDetails -> new UsernamePasswordAuthenticationToken(userDetails, password,
                userDetails.getAuthorities()));
    }
}
