package run.ikaros.metadata;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 外部、扫描或系统来源尝试更新字段时接受的来源说明。 */
public record AutomaticMetadataRequest(@Size(max = 10000) String value, @NotNull MetadataSource source,
                                       @Size(max = 255) String sourceReference) { }
