package run.ikaros.music; import jakarta.validation.constraints.Min; public record UpdateMusicPlaybackRequest(@Min(0) long positionMillis) {}
