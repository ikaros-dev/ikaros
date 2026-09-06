package run.ikaros.metadata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 写入元数据字段值时接受的内容。 */
public record MetadataValueRequest(@NotBlank @Size(max = 10000) String value) { }
