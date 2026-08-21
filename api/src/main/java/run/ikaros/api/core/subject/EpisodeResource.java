package run.ikaros.api.core.subject;

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeResource {
    private UUID attachmentId;
    private UUID parentAttachmentId;
    private UUID episodeId;
    private String url;
    private boolean canRead;
    private String name;
    /**
     * Such as 1080p 720p.
     */
    private Set<String> tags;
}
