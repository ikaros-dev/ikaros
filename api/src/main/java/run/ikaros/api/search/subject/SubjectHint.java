package run.ikaros.api.search.subject;

import org.springframework.util.Assert;
import run.ikaros.api.store.enums.SubjectType;

public record SubjectHint(
    Long id,
    String name,
    String nameCn,
    String infobox,
    String summary,
    Boolean nsfw,
    SubjectType type,
    Long airTime,
    String cover
) {
    public static final String ID_FIELD = "subject";

    /**
     * Construct.
     */
    public SubjectHint {
        // Assert.isTrue(id > 0, "'id' must gt 0.");
        Assert.hasText(name, "'name' must not be blank.");
        // Assert.hasText(infobox, "'infobox' must not be blank.");
        // Assert.hasText(summary, "'summary' must not be blank.");
        // Assert.notNull(nsfw, "'nsfw' must not be null.");
        // Assert.notNull(totalEpisodes, "'totalEpisodes' must not be null.");
        // Assert.notNull(type, "'type' must not be null.");
        // Assert.notNull(airTime, "'airTime' must not be null.");
    }
}
