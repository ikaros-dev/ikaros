package run.ikaros.server.core.subject;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.infra.utils.JsonUtils;
import run.ikaros.server.security.SecurityProperties;

@SpringBootTest
@AutoConfigureWebTestClient
class SubjectEndpointTest {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    WebTestClient webTestClient;
    @SpyBean
    SubjectService subjectService;
    @Autowired
    SecurityProperties securityProperties;

    private String username;
    private String password;

    @BeforeEach
    void setUp() {
        webTestClient = webTestClient.mutateWith(csrf());
        username = securityProperties.getInitializer().getMasterUsername();
        password = securityProperties.getInitializer().getMasterPassword();
    }

    @Test
    void getByIdWhenNotFound() {
        webTestClient.get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject/" + "10")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
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
                + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk()
            .expectBody(Subject.class);
    }


    private static Subject createSubjectInstance() {
        var subject = new Subject();
        subject.setName("subject-name-unit-test");
        subject.setType(SubjectType.ANIME);
        subject.setNsfw(Boolean.FALSE);
        subject.setInfobox("infobox-unit-test" + new Random(100).nextInt());
        subject.setNameCn("单元测试条目名");
        subject.setAirTime(LocalDateTime.now());

        var image = new SubjectImage();
        image.setCommon("https://ikaros.run/static/test.jpg");
        subject.setImage(image);

        var episodes = new ArrayList<Episode>();
        episodes.add(Episode.builder()
            .subjectId(Long.MAX_VALUE)
            .airTime(LocalDateTime.now())
            .name("ep-01")
            .nameCn("第一集").build());
        subject.setEpisodes(episodes)
            .setTotalEpisodes((long) episodes.size());
        return subject;
    }

    @Test
    void list() {
        webTestClient
            .get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/subjects/1/50")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @Disabled
    @SuppressWarnings("unchecked")
    void listWhenExists() {
        Subject subject = createSubjectInstance();

        try {
            webTestClient
                .post()
                .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject")
                .header(HttpHeaders.AUTHORIZATION, "Basic "
                    + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subject)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(entityExchangeResult -> {
                    Subject subject1 =
                        JsonUtils.json2obj(new String(
                                Objects.requireNonNull(entityExchangeResult.getResponseBody()),
                                StandardCharsets.UTF_8),
                            Subject.class);
                    Assertions.assertThat(subject1).isNotNull();
                    Assertions.assertThat(subject1.getId()).isNotZero();
                    subject.setId(subject1.getId());
                });

            webTestClient
                .get()
                .uri("/api/" + OpenApiConst.CORE_VERSION + "/subjects/1/50")
                .header(HttpHeaders.AUTHORIZATION, "Basic "
                    + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(entityExchangeResult -> {
                    Map pagingWrapMap = JsonUtils.json2obj(new String(
                            Objects.requireNonNull(entityExchangeResult.getResponseBody()),
                            StandardCharsets.UTF_8),
                        Map.class);
                    Assertions.assertThat(pagingWrapMap).isNotNull();
                    Object itemsJsonObj = pagingWrapMap.get("items");
                    Subject[] subjects =
                        JsonUtils.json2ObjArr(JsonUtils.obj2Json(itemsJsonObj),
                            new TypeReference<>() {
                            });
                    Assertions.assertThat(subjects).isNotNull();
                    Assertions.assertThat(subjects.length).isGreaterThan(0);
                    Subject subject1 = subjects[0];
                    Assertions.assertThat(subject1).isNotNull();
                    Assertions.assertThat(subject1.getId()).isEqualTo(subject.getId());
                });

        } finally {
            if (subject.getId() != null) {
                StepVerifier.create(subjectService.deleteById(subject.getId())).verifyComplete();
            }
        }

    }

    @Test
    void save() {
        Subject subject = createSubjectInstance();

        try {
            webTestClient
                .post()
                .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject")
                .header(HttpHeaders.AUTHORIZATION, "Basic "
                    + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subject)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(entityExchangeResult -> {
                    Subject subject1 =
                        JsonUtils.json2obj(new String(
                                Objects.requireNonNull(entityExchangeResult.getResponseBody()),
                                StandardCharsets.UTF_8),
                            Subject.class);
                    Assertions.assertThat(subject1).isNotNull();
                    Assertions.assertThat(subject1.getId()).isNotZero();
                    subject.setId(subject1.getId());
                });
        } finally {
            if (subject.getId() != null) {
                StepVerifier.create(subjectService.deleteById(subject.getId())).verifyComplete();
            }
        }

    }

    @Test
    void deleteById() {
        Subject subject = createSubjectInstance();

        try {
            webTestClient
                .post()
                .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject")
                .header(HttpHeaders.AUTHORIZATION, "Basic "
                    + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(subject)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(entityExchangeResult -> {
                    Subject subject1 =
                        JsonUtils.json2obj(new String(
                                Objects.requireNonNull(entityExchangeResult.getResponseBody()),
                                StandardCharsets.UTF_8),
                            Subject.class);
                    Assertions.assertThat(subject1).isNotNull();
                    Assertions.assertThat(subject1.getId()).isNotZero();
                    subject.setId(subject1.getId());
                });
        } finally {
            if (subject.getId() != null) {
                webTestClient.delete()
                    .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject/" + subject.getId())
                    .header(HttpHeaders.AUTHORIZATION, "Basic "
                        + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
                    .exchange()
                    .expectStatus().isOk();
            }
        }
    }
}