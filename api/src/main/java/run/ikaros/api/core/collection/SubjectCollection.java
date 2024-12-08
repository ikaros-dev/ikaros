package run.ikaros.api.core.collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.store.enums.SubjectType;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SubjectCollection {
    private Long id;
    @JsonProperty("user_id")
    private Long userId;
    @JsonProperty("subject_id")
    private Long subjectId;
    private CollectionType type;

    /**
     * User main group episode watching progress.
     */
    @JsonProperty("main_ep_progress")
    private Integer mainEpisodeProgress;

    /**
     * Whether it can be accessed without login.
     */
    @JsonProperty("is_private")
    private Boolean isPrivate;

    @JsonProperty("subject_type")
    private SubjectType subjectType;
    private String name;
    @JsonProperty("name_cn")
    private String nameCn;
    private String infobox;
    private String summary;
    /**
     * Not Safe/Suitable For Work.
     */
    private Boolean nsfw;
    private String cover;
    @JsonProperty("air_time")
    private LocalDateTime airTime;
    private String comment;
    /**
     * Subject score, from 0 to 10.
     */
    private Integer score;
}
