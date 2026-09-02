package run.ikaros.search;

import java.util.Map;
import java.util.UUID;

/** 由业务真相查询层提供的单条可重建投影输入。 */
public record SearchProjectionInput(UUID sourceId, long sourceVersion, Map<String, Object> fields) { }
