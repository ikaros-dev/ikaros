package run.ikaros.api.core.episode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.EpisodeGroup;

/**
 * Result of matching an attachment name against the regular expression chain.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class EpisodeSequenceRegularResult {
    private boolean matched;
    private String attachmentName;
    private EpisodeGroup epGroup;
    private Float sequence;
    private String matchedRuleName;
    private String matchedRegex;
}
