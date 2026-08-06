package run.ikaros.api.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 文件工具中的媒体策略委托测试。
 */
class FileUtilsTest {

    @Test
    void shouldParseOnlyKnownLastExtension() {
        assertThat(FileUtils.parseFilePostfix("movie.Final.MP4")).isEqualTo("mp4");
        assertThat(FileUtils.parseFilePostfix("cover.jpeg?size=large")).isEqualTo("jpeg");
        assertThat(FileUtils.parseFilePostfix("README")).isEmpty();
        assertThat(FileUtils.parseFilePostfix("archive.unknown")).isEmpty();
        assertThat(FileUtils.parseFilePostfix("trailing.")).isEmpty();
    }

    @Test
    void shouldDelegateMediaCategoryQueriesToPolicy() {
        assertThat(FileUtils.isVideo("movie.MKV")).isTrue();
        assertThat(FileUtils.isVideo("mp4")).isTrue();
        assertThat(FileUtils.isVoice("song.OPUS")).isTrue();
        assertThat(FileUtils.isImage("cover.AVIF")).isTrue();
        assertThat(FileUtils.isDocument("subtitle.srt")).isTrue();
        assertThat(FileUtils.isDocument("lyrics.lrc")).isTrue();
        assertThat(FileUtils.isVideo("movie.exe")).isFalse();
        assertThat(FileUtils.isDocument("report.docx")).isFalse();
    }
}
