package run.ikaros.photo;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetPhotoAlbumCoverRequest(@NotNull UUID photoId) { }
