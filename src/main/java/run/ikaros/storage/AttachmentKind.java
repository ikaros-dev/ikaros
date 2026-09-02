package run.ikaros.storage;

/**
 * Attachment 的业务角色，用于区分原始内容和可重建派生内容。
 */
public enum AttachmentKind {
    ORIGINAL,
    DERIVED,
    COVER,
    SUBTITLE
}
