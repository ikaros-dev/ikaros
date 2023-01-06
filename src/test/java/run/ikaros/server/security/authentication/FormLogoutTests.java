package run.ikaros.server.security.authentication;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.constant.AppConst;
import run.ikaros.server.infra.constant.SecurityConst;

@SpringBootTest
@AutoConfigureWebTestClient
public class FormLogoutTests {
    @Autowired
    WebTestClient webClient;
    @MockBean
    ReactiveUserDetailsService userDetailsService;
    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        when(userDetailsService.findByUsername("user"))
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
    void logout() {
        // login
        webClient
            .post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("username", "user")
                .with("password", "password"))
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().location(AppConst.LOGIN_SUCCESS_LOCATION);

        // logout
        webClient
            .get()
            .uri("/logout")
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectHeader().contentType(MediaType.TEXT_HTML);
    }
}
