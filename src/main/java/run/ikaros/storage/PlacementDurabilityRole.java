package run.ikaros.storage;

/** Placement 在长期保留策略中承担的职责，与访问成本层级正交。 */
public enum PlacementDurabilityRole {
    PRIMARY,
    REPLICA,
    ARCHIVE_BASE,
    PROMOTED_COPY
}
