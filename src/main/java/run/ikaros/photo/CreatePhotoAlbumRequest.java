package run.ikaros.photo; import jakarta.validation.constraints.NotBlank; public record CreatePhotoAlbumRequest(@NotBlank String name,String description) {}
