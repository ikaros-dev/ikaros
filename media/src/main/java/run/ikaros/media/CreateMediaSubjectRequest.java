package run.ikaros.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMediaSubjectRequest(@NotBlank String title, @NotNull MediaSubjectKind kind,
    String locale) {}
