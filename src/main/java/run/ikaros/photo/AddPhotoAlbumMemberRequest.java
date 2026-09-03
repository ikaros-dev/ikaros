package run.ikaros.photo; import jakarta.validation.constraints.NotNull; import java.util.UUID; public record AddPhotoAlbumMemberRequest(@NotNull UUID photoId) {}
