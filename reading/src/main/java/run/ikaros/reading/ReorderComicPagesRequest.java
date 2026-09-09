package run.ikaros.reading;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReorderComicPagesRequest(@NotBlank String chapterKey, @NotEmpty List<UUID> entryIds) {}
