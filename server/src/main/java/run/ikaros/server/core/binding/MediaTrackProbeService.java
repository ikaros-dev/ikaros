package run.ikaros.server.core.binding;

import java.nio.file.Path;
import java.util.List;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.MediaTrack;

/** 提供本地媒体文件内嵌轨道的只读探测能力. */
public interface MediaTrackProbeService {

    /**
     * 探测指定媒体文件中的音频与字幕轨道.
     *
     * @param mediaPath 已完成路径校验的媒体文件真实路径
     * @return 含轨道列表或失败原因的探测结果
     */
    Mono<ProbeResult> probe(Path mediaPath);

    /** 单个媒体文件的轨道探测结果. */
    record ProbeResult(List<MediaTrack> tracks, String failureReason) {

        /**
         * 创建成功探测结果.
         *
         * @param tracks 已探测到的轨道
         * @return 无失败原因的结果
         */
        public static ProbeResult success(List<MediaTrack> tracks) {
            return new ProbeResult(List.copyOf(tracks), null);
        }

        /**
         * 创建探测失败结果.
         *
         * @param failureReason 可展示的失败原因
         * @return 空轨道列表的失败结果
         */
        public static ProbeResult failure(String failureReason) {
            return new ProbeResult(List.of(), failureReason);
        }
    }
}
