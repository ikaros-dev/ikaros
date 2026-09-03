package run.ikaros.reading; import java.util.UUID; public record ReadingWorkView(UUID id,UUID resourceId,ReadingWorkKind kind,String originalLanguage) {}
