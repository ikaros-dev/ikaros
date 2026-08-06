package run.ikaros.api.core.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 权威媒体格式值表和文件名策略测试。
 */
class MediaFilePolicyTest {

    @Test
    void shouldExposeExactlyRequiredExtensions() {
        Set<String> extensions = Arrays.stream(MediaFileFormat.values())
            .flatMap(format -> format.extensions().stream())
            .collect(Collectors.toSet());

        assertThat(extensions).containsExactlyInAnyOrder(
            "mp4", "mkv", "mov", "webm", "flv", "f4v", "3gp", "3g2", "rmvb", "rm",
            "avi", "wmv", "asf", "ts", "m2ts", "mts", "tp", "vob", "dat", "m4v", "swf",
            "mpg", "mpeg", "mpe", "ogv", "qt", "trp", "m1v", "m2v", "ogg", "mxf", "wtv",
            "dvr-ms", "m4p", "amv", "viv", "ivf", "flc", "fli", "mjpeg",
            "mp3", "aac", "m4a", "oga", "flac", "ape", "wav", "alac", "aiff", "aif", "dsf",
            "dff", "opus", "amr", "wma", "mid", "midi", "ra", "m4r", "m4b", "mp2", "mp1",
            "aifc", "awb", "wv", "mpc", "mka", "tak", "tta", "shn", "gsm", "au", "snd",
            "vox", "qcp", "oma",
            "srt", "ass", "ssa", "vtt", "ttml", "dfxp", "sbv", "sub", "idx", "sup", "pgs",
            "smi", "smil", "scc", "sami", "rt", "pjs", "cap", "stl", "tds", "ttxt", "usf",
            "aqt", "jss", "lrc", "krc", "qrc", "trc", "ksc", "kaj", "tlrc", "skrc",
            "jpg", "jpeg", "jpe", "jfif", "png", "gif", "webp", "avif", "heic", "heif", "bmp",
            "dib", "tiff", "tif", "eps", "svg", "ico", "cur", "tga", "pcx", "wmf", "emf",
            "dds", "hdr", "exr", "pbm", "pgm", "ppm", "pnm", "fits", "fit", "jp2", "j2k",
            "jpf", "jpm", "jpg2", "jxl", "iff", "lbm", "mac", "pct", "pict", "xbm", "xpm",
            "cgm", "fpx", "wbm", "wbmp", "pcd", "ras", "sun");

        assertThat(extensions).doesNotContain(
            "cda", "ram", "txt", "xml", "xvid", "divx", "ai", "pdf", "psd", "psb", "pdd",
            "svgz", "dng", "cr2", "cr3", "crw", "nef", "nrw", "arw", "srf", "sr2", "orf",
            "ori", "rw2", "raf", "pef", "ptx", "dcr", "kdc", "k25", "mrw", "bay", "erf",
            "fff", "mef", "mos", "srw", "x3f", "dcm", "dicom");
    }

    @Test
    void shouldApplyLastExtensionFilenameGate() {
        assertThat(MediaFilePolicy.extractExtension("movie.MP4")).contains("mp4");
        assertThat(MediaFilePolicy.extractExtension("archive.tar.MKV")).contains("mkv");
        assertThat(MediaFilePolicy.extractExtension(null)).isEmpty();
        assertThat(MediaFilePolicy.extractExtension(" ")).isEmpty();
        assertThat(MediaFilePolicy.extractExtension("README")).isEmpty();
        assertThat(MediaFilePolicy.extractExtension("movie.")).isEmpty();
        assertThat(MediaFilePolicy.extractExtension(".profile")).isEmpty();
        assertThat(MediaFilePolicy.extractExtension("dir/movie.mp4")).isEmpty();
        assertThat(MediaFilePolicy.extractExtension("dir\\movie.mp4")).isEmpty();
        assertThat(MediaFilePolicy.extractExtension("movie.mp4.exe")).isEmpty();
    }

    @Test
    void shouldQueryCandidatesAndImmutableHintsFromAuthoritativeTable() {
        assertThat(MediaFilePolicy.formatsForExtension("JpEg"))
            .containsExactly(MediaFileFormat.JPEG);
        assertThat(MediaFilePolicy.formatsForExtension("sub"))
            .containsExactly(MediaFileFormat.MICRODVD, MediaFileFormat.VOBSUB);
        assertThat(MediaFilePolicy.formatsForExtension("exe")).isEmpty();
        assertThat(MediaFilePolicy.formatHints()).hasSize(MediaFileFormat.values().length);

        MediaFileFormatHint jpeg = MediaFilePolicy.hintsForExtension("jpg").get(0);
        assertThat(jpeg.category()).isEqualTo(MediaFileCategory.IMAGE);
        assertThat(jpeg.mimeType()).isEqualTo("image/jpeg");
        assertThatThrownBy(() -> jpeg.extensions().add("jpe"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldAllowBinaryMediaAcrossBinaryExtensions() {
        assertThat(allowed("png", MediaFileFormat.JPEG)).isTrue();
        assertThat(allowed("mp4", MediaFileFormat.MATROSKA)).isTrue();
        assertThat(allowed("mp4", MediaFileFormat.MP3)).isTrue();
        assertThat(allowed("mp4", MediaFileFormat.PNG)).isTrue();
    }

    @Test
    void shouldRequireExactTextFormatAndExplicitSubBranches() {
        assertThat(allowed("ass", MediaFileFormat.SRT)).isFalse();
        assertThat(allowed("srt", MediaFileFormat.ASS)).isFalse();
        assertThat(allowed("srt", MediaFileFormat.LRC)).isFalse();
        assertThat(allowed("ass", MediaFileFormat.SSA)).isFalse();
        assertThat(allowed("lrc", MediaFileFormat.SRT)).isFalse();
        assertThat(allowed("srt", MediaFileFormat.PNG)).isFalse();
        assertThat(allowed("sub", MediaFileFormat.MICRODVD)).isTrue();
        assertThat(allowed("sub", MediaFileFormat.VOBSUB)).isTrue();
        assertThat(allowed("sub", MediaFileFormat.SRT)).isFalse();
        assertThat(allowed("ttml", MediaFileFormat.TTML)).isTrue();
    }

    private boolean allowed(String extension, MediaFileFormat format) {
        return MediaFilePolicy.isDetectionAllowed(extension, new MediaFileDetectionResult(format));
    }
}
