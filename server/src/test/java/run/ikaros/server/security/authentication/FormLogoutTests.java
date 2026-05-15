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

@SpringBootTest
@AutoConfigureWebTestClient
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
public class FormLogoutTests {
    @Autowired
    WebTestClient webClient;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    SecurityProperties securityProperties;
    String username;
    String password;
    @Autowired
    MasterInitializer masterInitializer;

    @BeforeEach
    void setUp() {
        webClient = webClient.mutateWith(csrf());
        username = securityProperties.getInitializer().getMasterUsername();
        password = securityProperties.getInitializer().getMasterPassword();
        StepVerifier.create(masterInitializer.initialize()).verifyComplete();
    }

    @Test
    @Disabled
    void logout() {
        // login
        webClient
            .post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("username", username)
                .with("password", password))
            .exchange()
            .expectStatus().is2xxSuccessful();

        // logout
        webClient
            .post()
            .uri("/logout")
            .exchange()
            .expectStatus().is2xxSuccessful();
    }
}
