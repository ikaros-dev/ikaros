package run.ikaros.server.core.subject;


import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.server.infra.constant.OpenApiConst;
import run.ikaros.server.infra.constant.SecurityConst;
import run.ikaros.server.infra.utils.JsonUtils;
import run.ikaros.server.store.enums.SubjectRelationType;
import run.ikaros.server.store.repository.SubjectRelationRepository;


@SpringBootTest
@AutoConfigureWebTestClient
// @SuppressWarnings("unchecked")
class SubjectRelationEndpointTest {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    WebTestClient webTestClient;
    @Autowired
    SubjectRelationRepository subjectRelationRepository;
    @SpyBean
    ReactiveUserDetailsService userDetailsService;

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

    @AfterEach
    void tearDown() {
        StepVerifier.create(subjectRelationRepository.deleteAll()).verifyComplete();
    }

    @Test
    void findAllBySubjectId() {
        final long random = createSubjectRelationAndReturnOneRandomRelationSubId();

        webTestClient.get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject-relations/" + Long.MAX_VALUE)
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .consumeWith(entityExchangeResult -> {
                SubjectRelation[] subjectRelations = JsonUtils.json2ObjArr(new String(
                        Objects.requireNonNull(entityExchangeResult.getResponseBody()),
                        StandardCharsets.UTF_8),
                    new TypeReference<>() {
                    });
                Assertions.assertThat(subjectRelations).isNotNull();
                Assertions.assertThat(subjectRelations).isNotEmpty();

                Object subjectRelation1 = subjectRelations[0];
                SubjectRelation subjectRelation2 =
                    JsonUtils.json2obj(JsonUtils.obj2Json(subjectRelation1), SubjectRelation.class);
                Assertions.assertThat(subjectRelation2).isNotNull();
                Assertions.assertThat(subjectRelation2.getSubject()).isEqualTo(Long.MAX_VALUE);
                Assertions.assertThat(subjectRelation2.getRelationType())
                    .isEqualTo(SubjectRelationType.COMIC);
                Assertions.assertThat(subjectRelation2.getRelationSubjects()).contains(random);
            });
    }

    @Test
    void createSubjectRelation() {
        final long random = createSubjectRelationAndReturnOneRandomRelationSubId();

        webTestClient.get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject-relation/"
                + Long.MAX_VALUE + "/" + SubjectRelationType.COMIC.getCode())
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .consumeWith(entityExchangeResult -> {
                SubjectRelation subjectRelation1 =
                    JsonUtils.json2obj(new String(
                            Objects.requireNonNull(entityExchangeResult.getResponseBody()),
                            StandardCharsets.UTF_8),
                        SubjectRelation.class);
                Assertions.assertThat(subjectRelation1).isNotNull();
                Assertions.assertThat(subjectRelation1.getSubject()).isEqualTo(Long.MAX_VALUE);
                Assertions.assertThat(subjectRelation1.getRelationType())
                    .isEqualTo(SubjectRelationType.COMIC);
                Assertions.assertThat(subjectRelation1.getRelationSubjects()).contains(random);
            });

    }

    private long createSubjectRelationAndReturnOneRandomRelationSubId() {
        final long random = new Random().nextLong(1, 100000);
        SubjectRelation subjectRelation = SubjectRelation.builder()
            .subject(Long.MAX_VALUE)
            .relationType(SubjectRelationType.COMIC)
            .relationSubjects(Set.of(random, 9L))
            .build();

        webTestClient.post()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject-relation")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(subjectRelation)
            .exchange()
            .expectStatus().isOk();

        return random;
    }

    @Test
    void removeSubjectRelationWhenRelationSubjectsIsArr() {
        final long random = createSubjectRelationAndReturnOneRandomRelationSubId();

        webTestClient.get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject-relation/"
                + Long.MAX_VALUE + "/" + SubjectRelationType.COMIC.getCode())
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .consumeWith(entityExchangeResult -> {
                SubjectRelation subjectRelation1 =
                    JsonUtils.json2obj(new String(
                            Objects.requireNonNull(entityExchangeResult.getResponseBody()),
                            StandardCharsets.UTF_8),
                        SubjectRelation.class);
                Assertions.assertThat(subjectRelation1).isNotNull();
                Assertions.assertThat(subjectRelation1.getSubject()).isEqualTo(Long.MAX_VALUE);
                Assertions.assertThat(subjectRelation1.getRelationType())
                    .isEqualTo(SubjectRelationType.COMIC);
                Assertions.assertThat(subjectRelation1.getRelationSubjects()).contains(random);
            });

        webTestClient.delete()
            .uri(uriBuilder -> uriBuilder
                .path("/api/" + OpenApiConst.CORE_VERSION + "/subject-relation")
                .queryParam("subject_id", Long.MAX_VALUE)
                .queryParam("relation_type", SubjectRelationType.COMIC.getCode())
                .queryParam("relation_subjects", JsonUtils.obj2Json(Set.of(random)))
                .build())
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk();

        webTestClient.get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject-relation/"
                + Long.MAX_VALUE + "/" + SubjectRelationType.COMIC.getCode())
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .consumeWith(entityExchangeResult -> {
                SubjectRelation subjectRelation1 =
                    JsonUtils.json2obj(new String(
                            Objects.requireNonNull(entityExchangeResult.getResponseBody()),
                            StandardCharsets.UTF_8),
                        SubjectRelation.class);
                Assertions.assertThat(subjectRelation1).isNotNull();
                Assertions.assertThat(subjectRelation1.getSubject()).isEqualTo(Long.MAX_VALUE);
                Assertions.assertThat(subjectRelation1.getRelationType())
                    .isEqualTo(SubjectRelationType.COMIC);
                Assertions.assertThat(subjectRelation1.getRelationSubjects().contains(random))
                    .isFalse();
            });
    }

    @Test
    void removeSubjectRelationWhenRelationSubjectsIsNum() {
        final long random = createSubjectRelationAndReturnOneRandomRelationSubId();

        webTestClient.get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject-relation/"
                + Long.MAX_VALUE + "/" + SubjectRelationType.COMIC.getCode())
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .consumeWith(entityExchangeResult -> {
                SubjectRelation subjectRelation1 =
                    JsonUtils.json2obj(new String(
                            Objects.requireNonNull(entityExchangeResult.getResponseBody()),
                            StandardCharsets.UTF_8),
                        SubjectRelation.class);
                Assertions.assertThat(subjectRelation1).isNotNull();
                Assertions.assertThat(subjectRelation1.getSubject()).isEqualTo(Long.MAX_VALUE);
                Assertions.assertThat(subjectRelation1.getRelationType())
                    .isEqualTo(SubjectRelationType.COMIC);
                Assertions.assertThat(subjectRelation1.getRelationSubjects()).contains(random);
            });

        webTestClient.delete()
            .uri(uriBuilder -> uriBuilder
                .path("/api/" + OpenApiConst.CORE_VERSION + "/subject-relation")
                .queryParam("subject_id", Long.MAX_VALUE)
                .queryParam("relation_type", SubjectRelationType.COMIC.getCode())
                .queryParam("relation_subjects", JsonUtils.obj2Json(random))
                .build())
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk();

        webTestClient.get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject-relation/"
                + Long.MAX_VALUE + "/" + SubjectRelationType.COMIC.getCode())
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .consumeWith(entityExchangeResult -> {
                SubjectRelation subjectRelation1 =
                    JsonUtils.json2obj(new String(
                            Objects.requireNonNull(entityExchangeResult.getResponseBody()),
                            StandardCharsets.UTF_8),
                        SubjectRelation.class);
                Assertions.assertThat(subjectRelation1).isNotNull();
                Assertions.assertThat(subjectRelation1.getSubject()).isEqualTo(Long.MAX_VALUE);
                Assertions.assertThat(subjectRelation1.getRelationType())
                    .isEqualTo(SubjectRelationType.COMIC);
                Assertions.assertThat(subjectRelation1.getRelationSubjects().contains(random))
                    .isFalse();
            });
    }
}