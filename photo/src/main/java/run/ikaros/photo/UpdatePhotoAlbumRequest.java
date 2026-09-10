package run.ikaros.photo;

import jakarta.validation.constraints.NotBlank;

public record UpdatePhotoAlbumRequest(@NotBlank String name, String description) { }
