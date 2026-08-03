package run.ikaros.server.core.binding;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.HandlerBox;
import org.mp4parser.boxes.iso14496.part12.TrackBox;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.core.binding.MediaTrack;

/** 使用 ISO BMFF 解析器探测本地视频文件中的音频和字幕轨道。 */
@Slf4j
@Service
public class DefaultMediaTrackProbeService implements MediaTrackProbeService {

    /** ISO BMFF 容器扩展名。 */
    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".mp4", ".m4v", ".mov");
    /** 音频轨道处理器类型。 */
    private static final String AUDIO_HANDLER = "soun";
    /** 已知字幕轨道处理器类型。 */
    private static final List<String> SUBTITLE_HANDLERS = List.of("subt", "text", "sbtl", "clcp");

    @Override
    public Mono<ProbeResult> probe(Path mediaPath) {
        return Mono.fromCallable(() -> probeNow(mediaPath))
            .subscribeOn(Schedulers.boundedElastic());
    }

    private ProbeResult probeNow(Path mediaPath) {
        if (mediaPath == null || !Files.isRegularFile(mediaPath)) {
            return ProbeResult.failure("媒体文件不存在或不可访问");
        }
        if (!isSupportedContainer(mediaPath)) {
            return ProbeResult.failure("当前探测器不支持该容器");
        }
        try {
            if (!hasValidTopLevelBox(mediaPath)) {
                return ProbeResult.failure("媒体容器解析失败");
            }
        } catch (IOException exception) {
            log.debug("媒体轨道探测失败: {}", mediaPath.getFileName(), exception);
            return ProbeResult.failure("媒体容器解析失败");
        }
        try (IsoFile isoFile = new IsoFile(mediaPath.toString())) {
            List<TrackBox> trackBoxes = isoFile.getMovieBox().getBoxes(TrackBox.class);
            List<MediaTrack> tracks = java.util.stream.IntStream.range(0, trackBoxes.size())
                .mapToObj(index -> toMediaTrack(trackBoxes.get(index), index))
                .flatMap(java.util.Optional::stream)
                .toList();
            return tracks.isEmpty() ? ProbeResult.failure("未探测到音频或字幕轨道")
                : ProbeResult.success(tracks);
        } catch (IOException | RuntimeException exception) {
            log.debug("媒体轨道探测失败: {}", mediaPath.getFileName(), exception);
            return ProbeResult.failure("媒体容器解析失败");
        }
    }

    /** 验证顶层 box 的声明长度，阻止损坏文件诱导解析器分配超大内存。 */
    private boolean hasValidTopLevelBox(Path mediaPath) throws IOException {
        long fileSize = Files.size(mediaPath);
        if (fileSize < 8) {
            return false;
        }
        try (InputStream inputStream = Files.newInputStream(mediaPath)) {
            byte[] header = inputStream.readNBytes(16);
            if (header.length < 8) {
                return false;
            }
            long declaredSize = Integer.toUnsignedLong(ByteBuffer.wrap(header, 0, 4).getInt());
            long minimumSize = 8;
            if (declaredSize == 1) {
                if (header.length < 16) {
                    return false;
                }
                declaredSize = ByteBuffer.wrap(header, 8, 8).getLong();
                minimumSize = 16;
            } else if (declaredSize == 0) {
                declaredSize = fileSize;
            }
            return declaredSize >= minimumSize && declaredSize <= fileSize;
        }
    }

    private boolean isSupportedContainer(Path mediaPath) {
        String filename = mediaPath.getFileName().toString();
        int extensionStart = filename.lastIndexOf('.');
        return extensionStart >= 0 && SUPPORTED_EXTENSIONS.contains(
            filename.substring(extensionStart).toLowerCase(Locale.ROOT));
    }

    private java.util.Optional<MediaTrack> toMediaTrack(TrackBox trackBox, int index) {
        HandlerBox handlerBox = trackBox.getMediaBox().getHandlerBox();
        String handlerType = handlerBox.getHandlerType();
        String kind = handlerType.equals(AUDIO_HANDLER) ? "audio"
            : SUBTITLE_HANDLERS.contains(handlerType) ? "subtitle" : null;
        if (kind == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(MediaTrack.builder()
            .index(index)
            .kind(kind)
            .language(normalizeLanguage(trackBox.getMediaBox().getMediaHeaderBox().getLanguage()))
            .title(null)
            .defaultTrack((trackBox.getTrackHeaderBox().getFlags() & 1) != 0)
            .codec(null)
            .playable(false)
            .build());
    }

    private String normalizeLanguage(String language) {
        return language == null || language.isBlank() || language.equalsIgnoreCase("und") ? null : language;
    }
}
