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
import run.ikaros.server.infra.constant.SecurityConst;

/**
 * unit test for user authentication by form login.
 *
 * @author: li-guohao
 */
@SpringBootTest
@AutoConfigureWebTestClient
public class FormLoginTests {

    @Autowired
    WebTestClient webClient;
    @MockBean
    ReactiveUserDetailsService userDetailsService;

    @BeforeEach
    void setUp(@Autowired PasswordEncoder passwordEncoder) {
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
    void login() {
        webClient
            .post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("username", "user")
                .with("password", "password"))
            .exchange()
            .expectStatus()
            .is3xxRedirection();

        // MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        // formData.add("username", "user");
        // formData.add("password", "password");
        // webClient
        //     .post()
        //     .uri("/login")
        //     .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        //     .body(BodyInserters.fromFormData(formData))
        //     .exchange()
        //     .expectStatus()
        //     .is3xxRedirection();
    }

    @Test
    void loginWithUserNotExists() {
        webClient
            .post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("username", "not-exists-user")
                .with("password", "password"))
            .exchange()
            .expectStatus()
            .is5xxServerError();
    }

    @Test
    void loginWithUserPasswordIncorrect() {
        webClient
            .post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("username", "user")
                .with("password", "password-incorrect"))
            .exchange()
            .expectStatus()
            .is5xxServerError();
    }
}
