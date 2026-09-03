package run.ikaros.document; import jakarta.validation.constraints.NotNull; public record UpdateWorkingCopyRequest(@NotNull String content,String contentSchemaVersion,long expectedVersion) {}
