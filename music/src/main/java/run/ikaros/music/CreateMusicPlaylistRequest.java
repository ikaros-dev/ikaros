package run.ikaros.music;
import jakarta.validation.constraints.NotBlank;
public record CreateMusicPlaylistRequest(@NotBlank String name, String description) {}
