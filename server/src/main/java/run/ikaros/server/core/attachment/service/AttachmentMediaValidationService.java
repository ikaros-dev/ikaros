package run.ikaros.server.core.attachment.service;

import java.nio.file.Path;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.media.MediaFileDetectionResult;

/** 提供附件名称门禁和有限前缀真实格式检测. */
public interface AttachmentMediaValidationService {

    /**
     * 校验附件名称的最后扩展名是否在媒体白名单中.
     *
     * @param filename 附件名称
     * @return 规范化后的最后扩展名
     */
    String validateFilename(String filename);

    /**
     * 读取本地文件不超过 64 KiB 的前缀并确认真实格式.
     *
     * @param path 本地文件路径
     * @param filename 附件名称
     * @return 真实格式检测结果
     */
    Mono<MediaFileDetectionResult> validate(Path path, String filename);

    /**
     * 单次消费数据流的有限前缀并返回包含完整原始字节的回放流.
     *
     * @param content 附件数据流
     * @param filename 附件名称
     * @return 真实格式检测结果和单次回放流
     */
    Mono<ValidatedMediaStream> validate(Flux<DataBuffer> content, String filename);
}
