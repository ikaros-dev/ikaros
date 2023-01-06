package run.ikaros.server.security;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

/**
 * ikaros jwt authentication manager,
 * container jwt decode logic,
 * reference from AbstractUserDetailsReactiveAuthenticationManager.
 *
 * @see org.springframework.security.authentication.AbstractUserDetailsReactiveAuthenticationManager
 */
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        // TODO decode jwt and validate it
        return null;
    }
}
