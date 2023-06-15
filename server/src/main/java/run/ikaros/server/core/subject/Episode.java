package run.ikaros.server.core.subject;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Episode {
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

    private Set<EpisodeResource> resources;
}
