package run.ikaros.music;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddMusicQueueEntryRequest(@NotNull UUID trackId) {}
