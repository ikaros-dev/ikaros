package run.ikaros.media;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderMediaEpisodesRequest(@NotEmpty List<@NotNull @Valid UUID> episodeIds) {}
