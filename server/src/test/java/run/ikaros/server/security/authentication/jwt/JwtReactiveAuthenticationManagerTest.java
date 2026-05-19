package run.ikaros.server.security.authentication.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.exception.security.UserAuthenticationException;
import run.ikaros.api.infra.exception.user.UserNotFoundException;

class JwtReactiveAuthenticationManagerTest {

    @Mock
    private ReactiveUserDetailsService userDetailsService;
    @Mock
    private PasswordEncoder passwordEncoder;
    private JwtReactiveAuthenticationManager manager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        manager = new JwtReactiveAuthenticationManager(userDetailsService, passwordEncoder);
    }

    @Test
    void authenticate_success() {
        String username = "testuser";
        String rawPassword = "password";
        String encodedPassword = "encodedPassword";

        UserDetails userDetails = User.withUsername(username)
            .password(encodedPassword)
            .roles("USER")
            .build();

        when(userDetailsService.findByUsername(username))
            .thenReturn(Mono.just(userDetails));
        when(passwordEncoder.matches(rawPassword, encodedPassword))
            .thenReturn(true);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            username, rawPassword);

        StepVerifier.create(manager.authenticate(authentication))
            .assertNext(result -> {
                assertThat(result).isInstanceOf(UsernamePasswordAuthenticationToken.class);
                assertThat(result.getName()).isEqualTo(username);
                assertThat(result.getCredentials()).isEqualTo(rawPassword);
                assertThat(result.getAuthorities()).hasSize(1);
                assertThat(result.getAuthorities().iterator().next().getAuthority())
                    .isEqualTo("ROLE_USER");
            })
            .verifyComplete();
    }

    @Test
    void authenticate_userNotFound() {
        String username = "nonexistent";
        String rawPassword = "password";

        when(userDetailsService.findByUsername(username))
            .thenReturn(Mono.empty());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            username, rawPassword);

        StepVerifier.create(manager.authenticate(authentication))
            .expectError(UserNotFoundException.class)
            .verify();
    }

    @Test
    void authenticate_wrongPassword() {
        String username = "testuser";
        String rawPassword = "wrongPassword";
        String encodedPassword = "encodedPassword";

        UserDetails userDetails = User.withUsername(username)
            .password(encodedPassword)
            .roles("USER")
            .build();

        when(userDetailsService.findByUsername(username))
            .thenReturn(Mono.just(userDetails));
        when(passwordEncoder.matches(rawPassword, encodedPassword))
            .thenReturn(false);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            username, rawPassword);

        StepVerifier.create(manager.authenticate(authentication))
            .expectError(UserAuthenticationException.class)
            .verify();
    }
}
