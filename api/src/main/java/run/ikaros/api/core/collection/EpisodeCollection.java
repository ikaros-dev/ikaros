package run.ikaros.api.core.collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.EpisodeGroup;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class EpisodeCollection {
    private Long id;
    @JsonProperty("user_id")
    private Long userId;
    @JsonProperty("episode_id")
    private Long episodeId;
    /**
     * 是否已经看过.
     */
    private Boolean finish;
    /**
     * 观看进度，时间戳.
     */
    private Long progress;
    /**
     * 总时长，时间戳.
     */
    private Long duration;

    @JsonProperty("subject_id")
    private Long subjectId;
    private String name;
    @JsonProperty("name_cn")
    private String nameCn;
    private String description;
    @JsonProperty("air_time")
    private LocalDateTime airTime;
    private Integer sequence;
    @JsonProperty("ep_group")
    private EpisodeGroup group;
    @JsonProperty("update_time")
    private LocalDateTime updateTime;
}
