package run.ikaros.api.core.subject;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;
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
public class Episode {
    private UUID id;
    @JsonProperty("subject_id")
    private UUID subjectId;
    private String name;
    @JsonProperty("name_cn")
    private String nameCn;
    private String description;
    @JsonProperty("air_time")
    private LocalDateTime airTime;
    private Float sequence;
    private EpisodeGroup group;

    /**
     * Create a default episode instance.
     */
    public static Episode defaultEpisode(Long subjectId) {
        Episode episode = new Episode();
        episode.setAirTime(LocalDateTime.now());
        episode.setSequence(1F);
        episode.setGroup(EpisodeGroup.MAIN);
        episode.setDescription("Default episode description");
        episode.setName("Default episode name");
        episode.setNameCn("默认的剧集名称");
        if (subjectId != null) {
            episode.setSubjectId(subjectId);
        }
        return episode;
    }
}
