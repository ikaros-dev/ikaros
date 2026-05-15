package run.ikaros.server.security.authentication;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.security.MasterInitializer;
import run.ikaros.server.security.SecurityProperties;

/**
 * unit test for user authentication by form login.
 *
 * @author: li-guohao
 */
@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
@AutoConfigureWebTestClient
public class FormLoginTests {

    @Autowired
    WebTestClient webClient;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    MasterInitializer masterInitializer;
    @Autowired
    SecurityProperties securityProperties;
    String username;
    String password;

    @BeforeEach
    void setUp() {
        webClient = webClient.mutateWith(csrf());
        username = securityProperties.getInitializer().getMasterUsername();
        password = securityProperties.getInitializer().getMasterPassword();
        StepVerifier.create(masterInitializer.initialize()).verifyComplete();
    }


    @Test
    @Disabled
    void login() {
        webClient
            .post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("username", username)
                .with("password", password))
            .exchange()
            .expectStatus().is2xxSuccessful();

        // MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        // formData.add("username", username);
        // formData.add("password", username);
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
        final String notExistsUsername = "not-exists-user";

        webClient
            .post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("username", notExistsUsername)
                .with("password", "password"))
            .exchange()
            .expectStatus().is5xxServerError();
    }

    @Test
    void loginWithUserPasswordIncorrect() {
        webClient
            .post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("username", username)
                .with("password", "password-incorrect"))
            .exchange()
            .expectStatus().is4xxClientError();
    }

    @Test
    @Disabled
    void logout() {
        webClient
            .post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("username", username)
                .with("password", password))
            .exchange()
            .expectStatus().is2xxSuccessful();

        webClient
            .post()
            .uri("/logout")
            .exchange()
            .expectStatus().is2xxSuccessful();
    }
}
