package run.ikaros.server.core.file;

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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.file.FileConst;
import run.ikaros.api.core.file.FileHandler;
import run.ikaros.api.store.entity.FileEntity;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.security.SecurityProperties;
import run.ikaros.server.store.repository.FileRepository;

@SpringBootTest
@AutoConfigureWebTestClient
class FileEndpointTest {

    @Autowired
    WebTestClient webTestClient;
    @SpyBean
    ExtensionComponentsFinder extensionComponentsFinder;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    FileRepository fileRepository;
    @Autowired
    IkarosProperties ikarosProperties;
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
                + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
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
                + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
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
                + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
            .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void list() {
        webTestClient.get()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/files")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void deleteById() {
        webTestClient.delete()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/file" + "/-1")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().is5xxServerError();

        // upload a file
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", new ClassPathResource("core/file/UnitTestDocFile.TXT"))
            .contentType(MediaType.MULTIPART_FORM_DATA);
        multipartBodyBuilder.part("policyName", "LOCAL");

        webTestClient.post()
            .uri("/api/" + OpenApiConst.CORE_VERSION + "/files/upload")
            .header(HttpHeaders.AUTHORIZATION, "Basic "
                + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
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
                + HttpHeaders.encodeBasicAuth(username, password, StandardCharsets.UTF_8))
            .exchange()
            .expectStatus().isOk();
    }
}