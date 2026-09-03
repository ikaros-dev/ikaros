package run.ikaros.music; import jakarta.validation.constraints.NotNull; public record EndMusicPlaybackRequest(@NotNull MusicPlaybackState outcome) {}
