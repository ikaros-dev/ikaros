package run.ikaros.api.core.episode;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.EpisodeGroup;

/**
 * Regular expression rule for matching episode sequence from attachment filename.
 * Used by the Chain of Responsibility pattern.
 */
@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class EpisodeSequenceRegular {
    private UUID id;
    private String name;
    private String regex;
    private EpisodeGroup epGroup;
    private Float sequence;
    private Integer priority;
    private String description;
    private Boolean enabled;
    private LocalDateTime createTime;
}
