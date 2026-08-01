package run.ikaros.server.core.attachment.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

/**
 * 本地附件驱动路径校验器测试.
 */
class LocalAttachmentPathValidatorTest {
    /** 被测试的路径校验器. */
    private final LocalAttachmentPathValidator validator = new LocalAttachmentPathValidator();

    @ParameterizedTest
    @ValueSource(strings = {"E:/1/2", "E://1//2", "E:\\1\\2", "E:\\/1/2/3"})
    void toPathAcceptsSupportedWindowsSeparators(String path) {
        assertThatCode(() -> validator.toPath(path)).doesNotThrowAnyException();
    }

    @Test
    void toPathRejectsMixedSeparatorsBetweenWindowsPathSegments() {
        assertThatThrownBy(() -> validator.toPath("E:\\/1\\/2"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("路径段之间不能混用连续分隔符");
    }

    @Test
    void registerStoresCanonicalDriverRoot(@TempDir Path tempDir) throws IOException {
        UUID driverId = UUID.randomUUID();
        Path expectedRoot = tempDir.toRealPath();

        validator.register(driverId, tempDir.toString());

        StepVerifier.create(validator.validate(driverId, tempDir.toString()))
            .assertNext(path -> assertThat(path).isEqualTo(expectedRoot))
            .verifyComplete();
    }

    @Test
    void registerRejectsMissingRoot(@TempDir Path tempDir) {
        Path missingRoot = tempDir.resolve("missing");

        assertThatThrownBy(() -> validator.register(UUID.randomUUID(), missingRoot.toString()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("驱动根目录不存在或不可访问");
    }

    @Test
    void unregisterRevokesDriverAccess(@TempDir Path tempDir) {
        UUID driverId = UUID.randomUUID();
        validator.register(driverId, tempDir.toString());

        validator.unregister(driverId);

        StepVerifier.create(validator.validate(driverId, tempDir.toString()))
            .expectErrorSatisfies(error -> assertThat(error)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("本地附件驱动未启用或未注册"))
            .verify();
    }

    @Test
    void validateAllowsExistingPathInsideDriverRoot(@TempDir Path tempDir) throws IOException {
        UUID driverId = UUID.randomUUID();
        Path file = Files.writeString(tempDir.resolve("video.mkv"), "content");
        Path expectedFile = file.toRealPath();
        validator.register(driverId, tempDir.toString());

        StepVerifier.create(validator.validate(driverId, file.toString()))
            .assertNext(path -> assertThat(path).isEqualTo(expectedFile))
            .verifyComplete();
    }

    @Test
    void validateRejectsExistingPathOutsideDriverRoot(@TempDir Path tempDir) throws IOException {
        UUID driverId = UUID.randomUUID();
        Path driverRoot = Files.createDirectory(tempDir.resolve("driver"));
        Path outsideFile = Files.writeString(tempDir.resolve("secret.txt"), "secret");
        validator.register(driverId, driverRoot.toString());

        StepVerifier.create(validator.validate(driverId, outsideFile.toString()))
            .expectErrorSatisfies(error -> assertThat(error)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目标路径超出驱动根目录"))
            .verify();
    }

    @Test
    void validateRejectsSymbolicLinkEscapingDriverRoot(@TempDir Path tempDir) throws IOException {
        UUID driverId = UUID.randomUUID();
        Path driverRoot = Files.createDirectory(tempDir.resolve("driver"));
        Path outsideFile = Files.writeString(tempDir.resolve("secret.txt"), "secret");
        Path link = driverRoot.resolve("secret-link.txt");
        try {
            Files.createSymbolicLink(link, outsideFile);
        } catch (IOException | UnsupportedOperationException exception) {
            Assumptions.assumeTrue(false, "当前文件系统不允许创建符号链接");
        }
        validator.register(driverId, driverRoot.toString());

        StepVerifier.create(validator.validate(driverId, link.toString()))
            .expectErrorSatisfies(error -> assertThat(error)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("目标路径超出驱动根目录"))
            .verify();
    }
}
