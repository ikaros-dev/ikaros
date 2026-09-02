package run.ikaros.document; import java.util.UUID; public record DocumentView(UUID id,UUID resourceId,DocumentKind kind,UUID currentRevisionId) {}
