package run.ikaros.resource.api;

import java.util.Map;
import java.util.UUID;

public record ResourceSearchProjection(UUID resourceId, long sourceVersion, Map<String, Object> fields) { }
