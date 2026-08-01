package run.ikaros.server.core.statics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.infra.properties.IkarosProperties;

@ExtendWith(MockitoExtension.class)
class StaticServiceImplTest {

    @Mock
    private IkarosProperties ikarosProperties;

    private StaticServiceImpl staticService;
    private Path tempWorkDir;

    @BeforeEach
    void setUp() throws IOException {
        tempWorkDir = Files.createTempDirectory("ikaros-statics-test-");
        when(ikarosProperties.getWorkDir()).thenReturn(tempWorkDir);
        staticService = new StaticServiceImpl(ikarosProperties);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempWorkDir != null) {
            Files
                .walk(tempWorkDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        //
                    }
                });
        }
    }

    @Test
    void listStaticsFontsWhenDirNotExists() {
        StepVerifier
            .create(staticService.listStaticsFonts())
            .expectNextCount(0)
            .verifyComplete();
    }

    @Test
    void listStaticsFontsWhenFontDirNotExists() throws IOException {
        Path staticsDir = tempWorkDir.resolve(AppConst.STATIC_DIR_NAME);
        Files.createDirectory(staticsDir);
        StepVerifier
            .create(staticService.listStaticsFonts())
            .expectNextCount(0)
            .verifyComplete();
    }

    @Test
    void listStaticsFontsWithFiles() throws IOException {
        Path staticsDir = tempWorkDir.resolve(AppConst.STATIC_DIR_NAME);
        Files.createDirectory(staticsDir);
        Path fontDir = staticsDir.resolve(AppConst.STATIC_FONT_DIR_NAME);
        Files.createDirectory(fontDir);
        Files.createFile(fontDir.resolve("NotoSansSC-Regular.otf"));
        Files.createFile(fontDir.resolve("NotoSerifSC-Regular.otf"));

        StepVerifier
            .create(staticService
                .listStaticsFonts()
                .collectList())
            .assertNext(fonts -> {
                assertThat(fonts).hasSize(2);
                assertThat(fonts).anyMatch(f -> f.contains("NotoSansSC-Regular.otf"));
                assertThat(fonts).anyMatch(f -> f.contains("NotoSerifSC-Regular.otf"));
            })
            .verifyComplete();
    }

    @Test
    void listStaticsFontsReturnsCorrectUrlPrefix() throws IOException {
        Path staticsDir = tempWorkDir.resolve(AppConst.STATIC_DIR_NAME);
        Files.createDirectory(staticsDir);
        Path fontDir = staticsDir.resolve(AppConst.STATIC_FONT_DIR_NAME);
        Files.createDirectory(fontDir);
        Files.createFile(fontDir.resolve("test.woff2"));

        StepVerifier
            .create(staticService.listStaticsFonts())
            .assertNext(font -> assertThat(font).startsWith(
                "/static/" + AppConst.STATIC_FONT_DIR_NAME + "/"))
            .verifyComplete();
    }

    @Test
    void listStaticsFontsWhenFontDirIsEmpty() throws IOException {
        Path staticsDir = tempWorkDir.resolve(AppConst.STATIC_DIR_NAME);
        Files.createDirectory(staticsDir);
        Path fontDir = staticsDir.resolve(AppConst.STATIC_FONT_DIR_NAME);
        Files.createDirectory(fontDir);

        StepVerifier
            .create(staticService
                .listStaticsFonts()
                .collectList())
            .assertNext(fonts -> assertThat(fonts).isEmpty())
            .verifyComplete();
    }
}
