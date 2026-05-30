package run.ikaros.api.core.subject;

import java.util.List;
import java.util.Map;
import run.ikaros.api.core.tag.Tag;

public record SubjectRecord(
    Subject subject,
    List<EpisodeRecord> episodes,
    List<Tag> tags,
    List<SubjectSync> syncs,
    Map<String, String> extra
) {
}
