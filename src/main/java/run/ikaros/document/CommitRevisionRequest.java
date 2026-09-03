package run.ikaros.document; import jakarta.validation.constraints.NotNull; public record CommitRevisionRequest(@NotNull String content,String contentSchemaVersion,long expectedVersion) {}
