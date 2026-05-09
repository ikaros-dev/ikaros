package run.ikaros.api.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FileUtilsMethodsTest {

    @Test
    void parseFilePostfixShouldExtractExtension() {
        // Returns extension without dot
        assertThat(FileUtils.parseFilePostfix("test.mp4")).isEqualTo("mp4");
        assertThat(FileUtils.parseFilePostfix("test.jpg")).isEqualTo("jpg");
        assertThat(FileUtils.parseFilePostfix("document.pdf")).isEqualTo("pdf");
    }

    @Test
    void parseFilePostfixShouldHandleUrlWithQuery() {
        assertThat(FileUtils.parseFilePostfix("test.mp4?v=1")).isEqualTo("mp4");
    }

    @Test
    void parseFileNameWithoutPostfixShouldRemoveExtension() {
        assertThat(FileUtils.parseFileNameWithoutPostfix("test.mp4"))
            .isEqualTo("test");
        assertThat(FileUtils.parseFileNameWithoutPostfix("document.pdf"))
            .isEqualTo("document");
    }

    @Test
    void parseFileNameShouldExtractFileName() {
        // Uses "/" as separator
        assertThat(FileUtils.parseFileName("path/to/test.mp4"))
            .isEqualTo("test.mp4");
        assertThat(FileUtils.parseFileName("test.mp4"))
            .isEqualTo("test.mp4");
    }

    @Test
    void formatDirNameShouldRemoveSpecialChars() {
        String result = FileUtils.formatDirName("test:dir*name");
        assertThat(result).isEqualTo("testdirname");
    }

    @Test
    void isImageShouldReturnTrueForImageExtensions() {
        assertThat(FileUtils.isImage("test.jpg")).isTrue();
        assertThat(FileUtils.isImage("test.png")).isTrue();
        assertThat(FileUtils.isImage("test.gif")).isTrue();
    }

    @Test
    void isImageShouldReturnFalseForNonImage() {
        assertThat(FileUtils.isImage("test.mp4")).isFalse();
        assertThat(FileUtils.isImage("test.pdf")).isFalse();
    }

    @Test
    void isVideoShouldReturnTrueForVideoExtensions() {
        assertThat(FileUtils.isVideo("test.mp4")).isTrue();
        assertThat(FileUtils.isVideo("test.mkv")).isTrue();
    }

    @Test
    void isVideoShouldReturnFalseForNonVideo() {
        assertThat(FileUtils.isVideo("test.jpg")).isFalse();
    }

    @Test
    void isVoiceShouldReturnTrueForVoiceExtensions() {
        assertThat(FileUtils.isVoice("test.mp3")).isTrue();
        assertThat(FileUtils.isVoice("test.flac")).isTrue();
    }

    @Test
    void isVoiceShouldReturnFalseForNonVoice() {
        assertThat(FileUtils.isVoice("test.mp4")).isFalse();
    }

    @Test
    void isDocumentShouldReturnTrueForDocumentExtensions() {
        assertThat(FileUtils.isDocument("test.txt")).isTrue();
        assertThat(FileUtils.isDocument("test.doc")).isTrue();
        assertThat(FileUtils.isDocument("test.md")).isTrue();
    }

    @Test
    void isDocumentShouldReturnFalseForNonDocument() {
        assertThat(FileUtils.isDocument("test.mp4")).isFalse();
    }

    @Test
    void buildAppUploadFileBasePathShouldContainDateParts() {
        LocalDateTime time = LocalDateTime.of(2024, 1, 15, 10, 30);
        String path = FileUtils.buildAppUploadFileBasePath("base", time);
        assertThat(path).contains("2024");
        assertThat(path).contains("15");
    }
}
