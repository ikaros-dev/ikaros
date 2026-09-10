package run.ikaros.reading;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateEbookImportRequest(@NotNull UUID attachmentId, String title, String language) {}
