package run.ikaros.server.core.subject;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.enums.CollectionStatus;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Subject {
    private SubjectEntity entity;
    private List<EpisodeEntity> episodes;
    @JsonProperty("total_episodes")
    private Long totalEpisodes;
    private CollectionStatus collection;
}
