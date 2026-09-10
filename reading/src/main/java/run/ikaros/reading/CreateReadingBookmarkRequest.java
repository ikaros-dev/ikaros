package run.ikaros.reading;

import jakarta.validation.constraints.NotBlank;

public record CreateReadingBookmarkRequest(@NotBlank String locatorKind,
    @NotBlank String locatorValue, String contentVersion, String label) {}
