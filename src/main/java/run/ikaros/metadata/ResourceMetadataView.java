package run.ikaros.metadata;

import java.util.UUID;

/** 可供资源详情解释字段来源和覆盖状态的元数据视图。 */
public record ResourceMetadataView(UUID id, String fieldKey, String value, MetadataSource source,
                                   String sourceReference, boolean manuallyLocked, boolean applied) { }
