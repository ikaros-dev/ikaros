package run.ikaros.reading;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateComicImportRequest(@NotNull UUID attachmentId, String title, String language) {}
