package run.ikaros.photo;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReorderPhotoAlbumRequest(@NotEmpty List<UUID> photoIds) { }
