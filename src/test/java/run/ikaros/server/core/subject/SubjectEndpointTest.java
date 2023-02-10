package run.ikaros.server.core.subject;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.constant.OpenApiConst;
import run.ikaros.server.infra.constant.SecurityConst;

@SpringBootTest
@AutoConfigureWebTestClient
class SubjectEndpointTest {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    WebTestClient webTestClient;
    @SpyBean
    ReactiveUserDetailsService userDetailsService;
    @SpyBean
    SubjectService subjectService;

    @BeforeEach
    void setUp() {
        when(userDetailsService.findByUsername("tomoki"))
            .thenReturn(Mono.just(
                User.builder()
                    .username("tomoki")
                    .password("password")
                    .passwordEncoder(passwordEncoder::encode)
                    .roles(SecurityConst.ROLE_MASTER)
                    .build()
            ));
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @Test
    void getByIdWhenNotFound() {
        webTestClient.get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject/" + "10")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void getById() {
        var exceptId = new Random().nextLong(1, Integer.MAX_VALUE);
        if (exceptId == 10) {
            exceptId++;
        }
        final var exceptSubject = Mono.just(new Subject()
            .setInfobox(String.valueOf(new Random().ints().findFirst().orElse(-1))));

        Mockito.when(subjectService.findById(exceptId))
            .thenAnswer((Answer<Mono<Subject>>) invocation -> exceptSubject);

        webTestClient.get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject/" + exceptId)
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk()
            .expectBody(Subject.class);
    }
}