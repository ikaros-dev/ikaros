package run.ikaros.server.core.subject;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.server.store.enums.CollectionStatus;
import run.ikaros.server.store.enums.SubjectType;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Subject {
    private SubjectType type;
    private String name;
    @JsonProperty("name_cn")
    private String nameCn;
    private String infobox;
    private String platform;
    private String summary;
    /**
     * Can search by anonymous access.
     */
    private Boolean nsfw;
    @JsonProperty("bgmtv_id")
    private Long bgmtvId;

    private SubjectImage image;
    private List<Episode> episodes;
    @JsonProperty("total_episodes")
    private Long totalEpisodes;
    @JsonProperty("collection_status")
    private CollectionStatus collectionStatus;
}
