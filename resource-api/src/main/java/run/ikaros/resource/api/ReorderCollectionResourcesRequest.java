package run.ikaros.resource.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Collection 成员顺序请求。 */
public record ReorderCollectionResourcesRequest(@NotEmpty @Size(max = 100) List<UUID> resourceIds) { }
