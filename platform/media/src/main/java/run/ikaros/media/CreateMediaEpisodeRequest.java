package run.ikaros.media;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateMediaEpisodeRequest(@Min(0) int episodeNumber, Integer absoluteNumber,
    @NotBlank String title, String locale) {}
