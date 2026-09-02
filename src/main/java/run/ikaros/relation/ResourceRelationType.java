package run.ikaros.relation;

/**
 * Resource 间的明确、有方向业务关系类型。
 */
public enum ResourceRelationType {
    CONTAINS,
    PART_OF,
    PREQUEL_TO,
    SEQUEL_TO,
    ADAPTATION_OF,
    VERSION_OF,
    DERIVED_FROM,
    RELATED_TO
}
