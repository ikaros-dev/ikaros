package run.ikaros.music;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMusicPlaylistRequest(@NotBlank @Size(max = 512) String name,
    @Size(max = 4096) String description, Long expectedVersion) {}
