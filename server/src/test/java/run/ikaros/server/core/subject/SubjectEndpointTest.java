package run.ikaros.server.core.subject;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.vo.FindSubjectCondition;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.infra.utils.JsonUtils;
import run.ikaros.server.security.MasterInitializer;
import run.ikaros.server.security.SecurityProperties;

@SpringBootTest
@AutoConfigureWebTestClient
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class SubjectEndpointTest {

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    WebTestClient webTestClient;
    @MockitoSpyBean
    SubjectService subjectService;
    @Autowired
    SecurityProperties securityProperties;
    @Autowired
    MasterInitializer masterInitializer;

    private String username;
    private String password;

    @BeforeEach
    void setUp() {
        webTestClient = webTestClient.mutateWith(csrf());
        username = securityProperties.getInitializer().getMasterUsername();
        password = securityProperties.getInitializer().getMasterPassword();
        StepVerifier.create(masterInitializer.initialize()
            .onErrorResume(e -> Mono.empty())).verifyComplete();
    }

    @Test
    @Disabled
    void getByIdWhenNotFound() {
        webTestClient.get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/subject/" + UuidV7Utils.generateUuid())
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void getById() {
        var exceptId = UuidV7Utils.generateUuid();
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

    @Test
    void listByConditionParsesKeywordAndDeduplicatesTypes() {
        PagingWrap<Subject> response = new PagingWrap<>(1, 10, 0L, List.of());
        Mockito.doReturn(Mono.just(response))
            .when(subjectService).listEntitiesByCondition(Mockito.any());
        String keyword = Base64.getEncoder()
            .encodeToString("统一搜索".getBytes(StandardCharsets.UTF_8));

        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/" + OpenApiConst.CORE_VERSION + "/subjects/condition")
                .queryParam("keyword", keyword)
                .queryParam("types", " VIDEO,ANIME,VIDEO,REAL ")
                .queryParam("nsfw", "false")
                .build())
            .header(HttpHeaders.AUTHORIZATION, basicAuthorization())
            .exchange()
            .expectStatus().isOk();

        ArgumentCaptor<FindSubjectCondition> conditionCaptor =
            ArgumentCaptor.forClass(FindSubjectCondition.class);
        Mockito.verify(subjectService).listEntitiesByCondition(conditionCaptor.capture());
        FindSubjectCondition condition = conditionCaptor.getValue();
        Assertions.assertThat(condition.getKeyword()).isEqualTo("统一搜索");
        Assertions.assertThat(condition.getTypes())
            .containsExactly(SubjectType.VIDEO, SubjectType.ANIME, SubjectType.REAL);
        Assertions.assertThat(condition.getNsfw()).isFalse();
    }

    @Test
    void listByConditionKeepsTypeWhenTypesIsEmpty() {
        PagingWrap<Subject> response = new PagingWrap<>(1, 10, 0L, List.of());
        Mockito.doReturn(Mono.just(response))
            .when(subjectService).listEntitiesByCondition(Mockito.any());

        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/" + OpenApiConst.CORE_VERSION + "/subjects/condition")
                .queryParam("type", "MUSIC")
                .queryParam("types", " ,  ")
                .build())
            .header(HttpHeaders.AUTHORIZATION, basicAuthorization())
            .exchange()
            .expectStatus().isOk();

        ArgumentCaptor<FindSubjectCondition> conditionCaptor =
            ArgumentCaptor.forClass(FindSubjectCondition.class);
        Mockito.verify(subjectService).listEntitiesByCondition(conditionCaptor.capture());
        Assertions.assertThat(conditionCaptor.getValue().getType()).isEqualTo(SubjectType.MUSIC);
        Assertions.assertThat(conditionCaptor.getValue().getTypes()).isEmpty();
    }

    @Test
    void listByConditionReturnsBadRequestForInvalidTypes() {
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/" + OpenApiConst.CORE_VERSION + "/subjects/condition")
                .queryParam("types", "VIDEO,INVALID")
                .build())
            .header(HttpHeaders.AUTHORIZATION, basicAuthorization())
            .exchange()
            .expectStatus().isBadRequest();

        Mockito.verify(subjectService, Mockito.never())
            .listEntitiesByCondition(Mockito.any());
    }

    private String basicAuthorization() {
        return "Basic " + HttpHeaders.encodeBasicAuth(
            username, password, StandardCharsets.UTF_8);
    }


    private static Subject createSubjectInstance() {
        var subject = new Subject();
        subject.setName("subject-name-unit-test");
        subject.setType(SubjectType.ANIME);
        subject.setNsfw(Boolean.FALSE);
        subject.setInfobox("infobox-unit-test" + new Random(100).nextInt());
        subject.setNameCn("单元测试条目名");
        subject.setAirTime(LocalDateTime.now());

        var episodes = new ArrayList<Episode>();
        episodes.add(Episode.builder()
            .subjectId(UuidV7Utils.generateUuid())
            .airTime(LocalDateTime.now())
            .name("ep-01")
            .nameCn("第一集")
            .group(EpisodeGroup.MAIN)
            .build());
        return subject;
    }

    @Test
    @Disabled
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
                    Assertions.assertThat(subject1.getId()).isNotNull();
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
                    Assertions.assertThat(subject1.getId()).isNotNull();
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
                    Assertions.assertThat(subject1.getId()).isNotNull();
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
