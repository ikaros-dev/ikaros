package run.ikaros.server.security.authentication;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.constant.SecurityConst;

/**
 * unit test for user authentication by form login.
 *
 * @author: li-guohao
 */
@Disabled
@SpringBootTest
@AutoConfigureWebTestClient
public class FormLoginTests {

    @Autowired
    WebTestClient webClient;
    @MockBean
    ReactiveUserDetailsService userDetailsService;

    @BeforeEach
    void setUp(@Autowired PasswordEncoder passwordEncoder) {
        when(userDetailsService.findByUsername(Mockito.anyString()))
            .thenReturn(Mono.just(
                User.builder()
                    .username("user")
                    .password("password")
                    .passwordEncoder(passwordEncoder::encode)
                    .roles(SecurityConst.DEFAULT_ROLE)
                    .build()
            ));

        webClient = webClient.mutateWith(csrf());
    }

    @Test
    void login() {
        webClient.post()
            .uri("/login")
            .bodyValue("username=user&password=password")
            .exchange()
            .expectStatus()
            .is2xxSuccessful();
    }
}
