package run.ikaros.api.core.music;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

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
    private UUID id;
    /**
     * 原始名称.
     */
    private String name;
    /**
     * 中文名称.
     */
    @JsonProperty("name_cn")
    private String nameCn;
    /**
     * 封面URL.
     */
    private String cover;
    /**
     * 专辑描述.
     */
    private String description;
    /**
     * 发行时间.
     */
    @JsonProperty("air_time")
    private LocalDateTime airTime;
    /**
     * 评分.
     */
    private Float score;
    /**
     * 排名.
     */
    private Integer rank;
    /**
     * 是否 NSFW.
     */
    private Boolean nsfw;
    /**
     * 歌曲数量.
     */
    @JsonProperty("song_count")
    private Long songCount;
}
