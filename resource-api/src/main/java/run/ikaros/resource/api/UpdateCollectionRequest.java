package run.ikaros.resource.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 编辑 Collection 的请求数据。 */
public record UpdateCollectionRequest(@NotBlank @Size(max = 256) String name,
                                      @Size(max = 2000) String description,
                                      long expectedVersion) { }
