package run.ikaros.music; import jakarta.validation.constraints.NotNull; import java.util.UUID; public record StartMusicPlaybackRequest(@NotNull UUID sourceId,UUID queueId,long positionMillis) {}
