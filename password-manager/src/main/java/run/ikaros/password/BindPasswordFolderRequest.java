package run.ikaros.password; import jakarta.validation.constraints.NotNull; import java.util.UUID; public record BindPasswordFolderRequest(@NotNull UUID folderId) {}
