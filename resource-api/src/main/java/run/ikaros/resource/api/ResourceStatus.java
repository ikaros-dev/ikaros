package run.ikaros.resource.api;

/** Integer lifecycle status persisted by the Resource owner. */
public enum ResourceStatus {
    DELETED(0),
    ACTIVE(1),
    TRASHED(2),
    ARCHIVED(3),
    FROZEN(4),
    UNFREEZING(5);

    private final int code;

    ResourceStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static ResourceStatus fromCode(int code) {
        for (ResourceStatus status : values()) {
            if (status.code == code) return status;
        }
        throw new IllegalArgumentException("Unknown Resource status code: " + code);
    }
}
