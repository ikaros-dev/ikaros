package run.ikaros.server.core.subject;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
    private Long id;
    @Schema(requiredMode = REQUIRED)
    private SubjectType type;

    @Schema(requiredMode = REQUIRED)
    private String name;
    @JsonProperty("name_cn")
    private String nameCn;
    private String infobox;
    private String platform;
    private String summary;
    /**
     * Can search by anonymous access.
     */
    @Schema(requiredMode = REQUIRED)
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