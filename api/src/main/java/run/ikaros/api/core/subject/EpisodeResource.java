package run.ikaros.api.core.subject;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import run.ikaros.api.core.binding.MediaTrack;

/** 剧集可播放或可阅读的附件资源投影。 */
@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeResource {
    /** 资源附件标识。 */
    private UUID attachmentId;
    /** 父级附件标识。 */
    private UUID parentAttachmentId;
    /** 所属剧集标识。 */
    private UUID episodeId;
    /** 资源访问地址。 */
    private String url;
    /** 当前用户是否可阅读该资源。 */
    private boolean canRead;
    /** 资源名称。 */
    private String name;
    /**
     * Such as 1080p 720p.
     */
    private Set<String> tags;
    /** 媒体资源中可选择的内嵌轨道。 */
    private List<MediaTrack> tracks;
    /** 资源是否为图片序列。 */
    private boolean imageSequence;
}
