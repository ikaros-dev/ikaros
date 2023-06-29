package run.ikaros.api.search.file;

import jakarta.annotation.Nullable;
import org.springframework.util.Assert;
import run.ikaros.api.store.enums.FileType;

public record FileHint(
    Long id,
    String name,
    String originalPath,
    String url,
    FileType type,
    @Nullable String originalName
) {
    public static final String ID_FIELD = "file";

    /**
     * Construct.
     */
    public FileHint {
        Assert.isTrue(id > 0, "'id' must gt 0.");
        Assert.hasText(name, "'name' must not be blank.");
        Assert.hasText(originalPath, "'originalPath' must not be blank.");
        Assert.hasText(url, "'url' must not be blank.");
        Assert.notNull(type, "'type' must not be null.");
    }
}
