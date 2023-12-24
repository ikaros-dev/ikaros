package run.ikaros.api.core.subject;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.EpisodeGroup;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class EpisodeMeta {
    private Long id;
    @JsonProperty("subject_id")
    private Long subjectId;
    private String name;
    @JsonProperty("name_cn")
    private String nameCn;
    private String description;
    @JsonProperty("air_time")
    private LocalDateTime airTime;
    private Integer sequence;
    private EpisodeGroup group;
}
