package run.ikaros.resource.api;

import java.util.UUID;

/** Collection 成员关系的只读视图。 */
public record CollectionResourceView(UUID resourceId, int position) { }
