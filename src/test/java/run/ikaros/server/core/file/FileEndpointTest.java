package run.ikaros.server.core.file;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.constant.AppConst;
import run.ikaros.server.infra.constant.OpenApiConst;
import run.ikaros.server.infra.constant.SecurityConst;
import run.ikaros.server.infra.properties.IkarosProperties;
import run.ikaros.server.infra.utils.FileUtils;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.FileEntity;
import run.ikaros.server.store.repository.FileRepository;

@SpringBootTest
@AutoConfigureWebTestClient
class FileEndpointTest {

    @Autowired
    WebTestClient webTestClient;
    @MockBean
    ReactiveUserDetailsService userDetailsService;
    @SpyBean
    ExtensionComponentsFinder extensionComponentsFinder;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    FileRepository fileRepository;
    @Autowired
    IkarosProperties ikarosProperties;

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
    void tearDown() throws IOException {
        fileRepository.deleteAll().block(AppConst.BLOCK_TIMEOUT);
        Path uploadDirPath = ikarosProperties.getWorkDir().resolve(FileConst.LOCAL_UPLOAD_DIR_NAME);
        FileUtils.deletePathAndContentIfExists(uploadDirPath);
    }

    @Test
    void uploadWhenNoAuth() {
        webTestClient.post()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/files/upload")
            .body(BodyInserters.fromMultipartData(new MultipartBodyBuilder().build()))
            .exchange()
            .expectStatus().isUnauthorized();

    }

    @Test
    void upload() {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", new ClassPathResource("core/file/UnitTestDocFile.TXT"))
            .contentType(MediaType.MULTIPART_FORM_DATA);
        multipartBodyBuilder.part("policyName", "LOCAL");

        webTestClient.post()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/files/upload")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void uploadWhenPolicyNotExists() {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", new ClassPathResource("core/file/UnitTestDocFile.TXT"))
            .contentType(MediaType.MULTIPART_FORM_DATA);
        multipartBodyBuilder.part("policyName", "NOT_EXISTS");

        webTestClient.post()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/files/upload")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void uploadWhenHandlerNotExists() {
        Mockito.doReturn(Collections.EMPTY_LIST)
            .when(extensionComponentsFinder).getExtensions(FileHandler.class);

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", new ClassPathResource("core/file/UnitTestDocFile.TXT"))
            .contentType(MediaType.MULTIPART_FORM_DATA);
        multipartBodyBuilder.part("policyName", "LOCAL");

        webTestClient.post()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/files/upload")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void list() {
        webTestClient.get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/files")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void deleteById() {
        webTestClient.delete()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/file" + "/-1")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isNotFound();

        // upload a file
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", new ClassPathResource("core/file/UnitTestDocFile.TXT"))
            .contentType(MediaType.MULTIPART_FORM_DATA);
        multipartBodyBuilder.part("policyName", "LOCAL");

        webTestClient.post()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/files/upload")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
            .exchange()
            .expectStatus().isOk();

        Long fileId = fileRepository.findOne(
                Example.of(FileEntity.builder().originalName("UnitTestDocFile.TXT").build()))
            .flatMap(entity -> Mono.just(entity.getId()))
            .block(AppConst.BLOCK_TIMEOUT);

        webTestClient.delete()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/file" + "/" + fileId)
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth("tomoki", "password", StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk();
    }
}