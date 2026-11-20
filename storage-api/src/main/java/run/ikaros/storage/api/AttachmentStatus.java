package run.ikaros.storage.api;

/** Numeric business lifecycle status for a logical Attachment. */
public enum AttachmentStatus {
    DELETED(0), ACTIVE(1), TRASHED(2), ARCHIVED(3), FROZEN(4), UNFREEZING(5);

    private final int code;

    AttachmentStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
