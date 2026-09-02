package run.ikaros.offline;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
public record CreateDownloadManifestRequest(@NotEmpty List<@Valid ManifestItemRequest> items) {}
