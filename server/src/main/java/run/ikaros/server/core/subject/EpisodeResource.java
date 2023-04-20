package run.ikaros.server.core.subject;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class EpisodeResource {
    private Long id;
    @JsonProperty("file_id")
    private Long fileId;
    @JsonProperty("episode_id")
    private Long episodeId;
    private String url;
    private String name;
    /**
     * Such as 1080p 720p.
     */
    private Set<String> tags;
}
