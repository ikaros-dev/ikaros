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
 * 音乐专辑 DTO.
 * 对应 SubjectType.MUSIC 类型的条目，包含专辑名称、封面、评分等基本信息以及歌曲数量.
 *
 * @author Nekoli
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Music {
    /**
     * 专辑ID.
     */
    private @Nullable UUID id;
    /**
     * 原始名称.
     */
    private @Nullable String name;
    /**
     * 中文名称.
     */
    @JsonProperty("name_cn")
    private @Nullable String nameCn;
    /**
     * 封面URL.
     */
    private @Nullable String cover;
    /**
     * 专辑描述.
     */
    private @Nullable String description;
    /**
     * 发行时间.
     */
    @JsonProperty("air_time")
    private @Nullable LocalDateTime airTime;
    /**
     * 评分.
     */
    private @Nullable Float score;
    /**
     * 排名.
     */
    private @Nullable Integer rank;
    /**
     * 是否 NSFW.
     */
    private @Nullable Boolean nsfw;
    /**
     * 歌曲数量.
     */
    @JsonProperty("song_count")
    private @Nullable Long songCount;
}
