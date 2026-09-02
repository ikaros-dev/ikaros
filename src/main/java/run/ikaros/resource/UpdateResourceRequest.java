package run.ikaros.resource;

import jakarta.validation.constraints.Size;

public record UpdateResourceRequest(long expectedVersion,
    @Size(max = 512) String primaryTitle, @Size(max = 4000) String summary) { }
