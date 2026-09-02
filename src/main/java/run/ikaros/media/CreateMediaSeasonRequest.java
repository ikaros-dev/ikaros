package run.ikaros.media;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateMediaSeasonRequest(@Min(0) int seasonNumber, @NotBlank String title, String locale) {}
