package run.ikaros.api.search.file;

import org.springframework.util.Assert;
import run.ikaros.api.store.enums.FilePlace;
import run.ikaros.api.store.enums.FileType;

public record FileHint(
    String name,
    String originalPath,
    String url,
    FileType type,
    FilePlace place
) {
    public static final String ID_FIELD = "file";

    /**
     * Construct.
     */
    public FileHint {
        Assert.hasText(name, "'name' must not be blank.");
        // Assert.hasText(originalPath, "'originalPath' must not be blank.");
        Assert.hasText(url, "'url' must not be blank.");
        // Assert.notNull(type, "'type' must not be null.");
        // Assert.notNull(place, "'place' must not be null.");
    }
}
