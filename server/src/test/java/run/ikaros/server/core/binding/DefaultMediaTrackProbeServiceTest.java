package run.ikaros.server.core.binding;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** ISO BMFF 内嵌轨道探测服务测试。 */
class DefaultMediaTrackProbeServiceTest {

    /** 待测试的轨道探测服务。 */
    private final MediaTrackProbeService mediaTrackProbeService = new DefaultMediaTrackProbeService();

    @Test
    void probe_returnsAudioAndSubtitleTracksInContainerOrder() throws Exception {
        Path fixture = fixture("track-fixture.mp4");

        MediaTrackProbeService.ProbeResult result = mediaTrackProbeService.probe(fixture).block();

        assertThat(result.failureReason()).isNull();
        assertThat(result.tracks()).extracting("index", "kind", "language", "defaultTrack", "codec")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1, "audio", "eng", true, null),
                org.assertj.core.groups.Tuple.tuple(2, "subtitle", "zho", true, null));
    }

    @Test
    void probe_returnsFailureWhenMovieHasNoTargetTrack() throws Exception {
        Path fixture = fixture("no-target-track.mp4");

        MediaTrackProbeService.ProbeResult result = mediaTrackProbeService.probe(fixture).block();

        assertThat(result.tracks()).isEmpty();
        assertThat(result.failureReason()).isEqualTo("未探测到音频或字幕轨道");
    }

    @Test
    void probe_returnsFailureForCorruptAndUnsupportedContainers(@TempDir Path tempDir) throws Exception {
        Path corrupt = fixture("corrupt.mp4");
        Path unsupported = Files.createFile(tempDir.resolve("video.mkv"));

        MediaTrackProbeService.ProbeResult corruptResult = mediaTrackProbeService.probe(corrupt).block();
        MediaTrackProbeService.ProbeResult unsupportedResult = mediaTrackProbeService.probe(unsupported).block();

        assertThat(corruptResult.tracks()).isEmpty();
        assertThat(corruptResult.failureReason()).isEqualTo("媒体容器解析失败");
        assertThat(unsupportedResult.tracks()).isEmpty();
        assertThat(unsupportedResult.failureReason()).isEqualTo("当前探测器不支持该容器");
    }

    private Path fixture(String filename) throws Exception {
        return Path.of(Objects.requireNonNull(
            getClass().getResource("/media/" + filename), "缺少媒体测试夹具").toURI());
    }
}
