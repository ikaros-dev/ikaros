package run.ikaros.music;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ReorderMusicQueueRequest(@NotEmpty @Size(max = 100) List<UUID> entryIds) {}
