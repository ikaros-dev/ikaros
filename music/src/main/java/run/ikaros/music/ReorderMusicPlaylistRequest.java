package run.ikaros.music;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReorderMusicPlaylistRequest(@NotEmpty List<UUID> entryIds) {}
