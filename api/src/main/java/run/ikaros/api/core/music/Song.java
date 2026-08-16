package run.ikaros.api.core.music;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

/**
 * 歌曲 DTO.
 * 对应 Episode 中属于音乐专辑的剧集，包含歌曲名称、曲目号、分组以及附件信息.
 *
 * @author Nekoli
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Song {
    /**
     * 歌曲ID.
     */
    private @Nullable UUID id;
    /**
     * 所属专辑ID.
     */
    @JsonProperty("subject_id")
    private @Nullable UUID subjectId;
    /**
     * 歌曲原始名称.
     */
    private @Nullable String name;
    /**
     * 歌曲中文名称.
     */
    @JsonProperty("name_cn")
    private @Nullable String nameCn;
    /**
     * 歌曲描述.
     */
    private @Nullable String description;
    /**
     * 发行时间.
     */
    @JsonProperty("air_time")
    private @Nullable LocalDateTime airTime;
    /**
     * 曲目号（排序用）.
     */
    private @Nullable Float sequence;
    /**
     * 分组类型，如 MAIN / OPENING_SONG / ORIGINAL_SOUND_TRACK 等.
     */
    private @Nullable String group;
    /**
     * 关联的附件ID（音频文件）.
     */
    @JsonProperty("attachment_id")
    private @Nullable UUID attachmentId;
    /**
     * 附件访问URL.
     */
    @JsonProperty("attachment_url")
    private @Nullable String attachmentUrl;
    /**
     * 音频时长（秒）.
     */
    private @Nullable Long duration;
}
